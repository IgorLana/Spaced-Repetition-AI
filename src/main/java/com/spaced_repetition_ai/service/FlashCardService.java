package com.spaced_repetition_ai.service;

import com.spaced_repetition_ai.dto.FlashcardRequestDTO;
import com.spaced_repetition_ai.dto.FlashcardResponseDTO;
import com.spaced_repetition_ai.entity.DeckEntity;
import com.spaced_repetition_ai.entity.FlashCardEntity;
import com.spaced_repetition_ai.entity.StandardFlashCardEntity;
import com.spaced_repetition_ai.entity.UserEntity;
import com.spaced_repetition_ai.exception.DatabaseException;
import com.spaced_repetition_ai.exception.NotFoundException;
import com.spaced_repetition_ai.model.DeckType;
import com.spaced_repetition_ai.model.FlashCard;
import com.spaced_repetition_ai.model.ReviewRating;
import com.spaced_repetition_ai.model.TextPromptStyle;
import com.spaced_repetition_ai.repository.DeckRepository;
import com.spaced_repetition_ai.repository.FlashCardRepository;
import com.spaced_repetition_ai.repository.StandardFlashCardRepository;
import com.spaced_repetition_ai.repository.UserRepository;
import com.spaced_repetition_ai.storage.AudioStorage;
import com.spaced_repetition_ai.storage.ImageStorage;
import org.slf4j.Logger;

import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
public class FlashCardService {

    private final TextGenerationService textGenerationService;
    private final ImageGenerationService imageGenerationService;
    private final AudioGenerationService audioGenerationService;
    private final FlashCardRepository flashCardRepository;
    private final DeckRepository deckRepository;
    private static final Logger log = LoggerFactory.getLogger(FlashCardService.class);
    private final UserRepository userRepository;
    private final StandardFlashCardRepository standardFlashCardRepository;
    private final ImageStorage imageStorage;
    private final AudioStorage audioStorage;
    private final AwsService awsService;

    private final Path storageBasePath = Paths.get("A:", "DeJavan", "spaced-repetition-ai", "Storage");

    public FlashCardService(TextGenerationService textGenerationService, ImageGenerationService imageGenerationService,
                            AudioGenerationService audioGenerationService, FlashCardRepository flashCardRepository,
                            DeckRepository deckRepository, UserRepository userRepository,
                            StandardFlashCardRepository standardFlashCardRepository,
                            ImageStorage imageStorage, AudioStorage audioStorage,
                            AwsService awsService) {
        this.textGenerationService = textGenerationService;
        this.imageGenerationService = imageGenerationService;
        this.audioGenerationService = audioGenerationService;
        this.flashCardRepository = flashCardRepository;
        this.deckRepository = deckRepository;
        this.userRepository = userRepository;
        this.standardFlashCardRepository = standardFlashCardRepository;
        this.imageStorage = imageStorage;
        this.audioStorage = audioStorage;
        this.awsService = awsService;
    }

    public void deleteFlashCard(Long flashCardId) {

        UserEntity usuarioLogado = getUsuarioLogado();

        FlashCardEntity flashCardEntity = flashCardRepository.findByIdAndDeckUserId(flashCardId, usuarioLogado.getId())
                        .orElseThrow(() -> new NotFoundException("FlashCard não encontrado com ID" + flashCardId));

        try {
            flashCardRepository.delete(flashCardEntity);
            log.info("FlashCard deletado com sucesso!");
        } catch (Exception e) {
            throw new DatabaseException("Erro ao deletar o flashcard", e);
        }
    }

    public void updateFlashCard(Long id, FlashcardRequestDTO dto) {
        UserEntity usuarioLogado = getUsuarioLogado();

        if(dto.getFront() == null || dto.getFront().trim().isEmpty()
        || dto.getBack() == null || dto.getBack().trim().isEmpty()) {
            FlashCardEntity ent = flashCardRepository.findByIdAndDeckUserId(id, usuarioLogado.getId())
                    .orElseThrow(() -> new NotFoundException("FlashCard não encontrado com id: " + id));
            ent.setFront(dto.getFront());
            ent.setBack(dto.getBack());
            ent.setImagePath(dto.getImagePath());
            ent.setAudioPath(dto.getAudioPath());
            try {
                flashCardRepository.save(ent);
                log.info("FlashCard atualizado com sucesso!");
            } catch (Exception e) {
                throw new DatabaseException("Erro ao atualizar o flashcard", e);
            }
        }else{
            throw new IllegalArgumentException("O FlashCard não pode ser atualizado com apenas um dos campos: front e back.");
        }
    }

    public void generateFlashCard(Long deckId, FlashcardRequestDTO dto) {

        if (dto.getFront() == null || dto.getFront().trim().isEmpty()) {
            throw new IllegalArgumentException("A frente do flashcard não pose ser vazia.");
        }
        LocalDateTime createdDate = LocalDateTime.now();
        LocalDateTime lastReview = LocalDateTime.now();
        LocalDateTime nextReview = createdDate.plusMinutes(1);

        UserEntity usuarioLogado = getUsuarioLogado();
        DeckEntity deckEntity = deckRepository.findByUserIdAndId(usuarioLogado.getId(), deckId)
                .orElseThrow(() -> new RuntimeException("Deck não encontrado ou não pertence ao usuário."));

        String finalImagePath = dto.getImagePath();
        String finalAudioPath = dto.getAudioPath();

        double easeFactor = deckEntity.getEaseFactor();
        int interval = 1;
        FlashCardEntity flashCardEntity = new FlashCardEntity(
                null, dto.getFront(), dto.getBack(), finalImagePath,
                finalAudioPath, createdDate,lastReview, nextReview,
                interval, ReviewRating.BOM, easeFactor, deckEntity
        );
        try {
            flashCardRepository.save(flashCardEntity);
        }catch (Exception e){
            throw new DatabaseException("Erro ao salvar o flashcard", e);
        }
        log.info("FlashCard salvo com sucesso!");
    }

    private record MediaData(String base64, String path) {}

    @Async
    public CompletableFuture<FlashcardResponseDTO> generateAiFlashCard(String prompt, Long deckId) throws IOException {

        UserEntity usuarioLogado = getUsuarioLogado();
        DeckEntity deckEntity = deckRepository.findByUserIdAndId(usuarioLogado.getId(), deckId)
                .orElseThrow(() -> new RuntimeException("Deck não encontrado ou não pertence ao usuário."));

        if(prompt == null || prompt.trim().isEmpty()){
            throw new IllegalArgumentException("O prompt do flashcard não pode ser vazio.");
        }

        final String standardizedPrompt = prompt.toLowerCase().trim();
        Optional<StandardFlashCardEntity> cachedCard = standardFlashCardRepository.findByPrompt(standardizedPrompt);
        if (cachedCard.isPresent()) {
            log.info("Cache hit para o prompt: {}. Retornando flashcard padrão.", standardizedPrompt);
            return createResponseFromStandardCard(cachedCard.get());
        }

        final CompletableFuture<MediaData> imageFuture = (deckEntity.getGenerateImage() && usuarioLogado.getBalance() >= 5)
                ? imageGenerationService.generateImageAsync(prompt, null, deckEntity.getImageStyle(), usuarioLogado.getId())
                .thenApply(imageData -> {
                    if (imageData == null || imageData.imageBytes() == null) return new MediaData(null, null);
                    String path = imageStorage.saveImage(UUID.randomUUID() + ".png", imageData.imageBytes());
                    String base64 = Base64.getEncoder().encodeToString(imageData.imageBytes());
                    return new MediaData(base64, path);
                })
                : CompletableFuture.completedFuture(new MediaData(null, null));

        CompletableFuture<FlashCard> textFuture = textGenerationService.generateTextFromJsonAsync(
                deckEntity.getDeckType() == DeckType.LANGUAGE ?
                        (TextPromptStyle.Language.getTemplate() + "Comece agora com a palavra: " + prompt + ". A lingua nativa é: " + deckEntity.getSourceLanguage().getLocaleCode() + " . E a lingua alvo para aprender é: " + deckEntity.getTargetLanguage().getLocaleCode()) :
                        (TextPromptStyle.GeneralFlashcards.getTemplate() + "Comece agora com a palavra: " + prompt)
        );

        CompletableFuture<MediaData> audioFuture = textFuture.thenCompose(card -> {
            if (deckEntity.getGenerateAudio() && usuarioLogado.getBalance() >= 1 && card != null) {
                return audioGenerationService.generateAudioAsync(card.getFront(), null, usuarioLogado.getId())
                        .thenApply(audioData -> { // Recebe os bytes do áudio
                            if (audioData == null || audioData.audioBytes() == null) return new MediaData(null, null);

                            String path = audioStorage.saveAudioFile(UUID.randomUUID() + ".wav", audioData.audioBytes());
                            String base64 = Base64.getEncoder().encodeToString(audioData.audioBytes());
                            return new MediaData(base64, path);
                        });
            }
            return CompletableFuture.completedFuture(new MediaData(null, null));
        });

        return CompletableFuture.allOf(imageFuture, textFuture, audioFuture)
                .thenApply(v -> {
                    MediaData imageData = imageFuture.join();
                    FlashCard card = textFuture.join();
                    MediaData audioData = audioFuture.join();

                    if (card == null) {
                        throw new RuntimeException("Falha ao gerar o texto do flashcard.");
                    }

                    CompletableFuture.runAsync(() ->
                            saveStandardFlashCards(prompt, card.getFront(), card.getBack(), imageData.path(), audioData.path())
                    ).exceptionally(ex -> {
                        log.error("Erro ao salvar flashcard padrão em background", ex);
                        return null;
                    });

                    log.info("FlashCard gerado, enviando resposta para o usuário.");
                    return new FlashcardResponseDTO(
                            card.getFront(),
                            card.getBack(),
                            imageData.base64(),
                            audioData.base64()
                    );
                });

    }

    public void saveStandardFlashCards(String prompt, String front, String back, String imagePath, String audioPath) {
        if (prompt != null && !prompt.trim().isEmpty() && front != null && back != null) {

            String standardizedPrompt = prompt.toLowerCase().trim();

            if (standardFlashCardRepository.findByPrompt(standardizedPrompt).isEmpty()) {
                StandardFlashCardEntity standardFlashCardEntity = new StandardFlashCardEntity(null, front, back, imagePath, audioPath, standardizedPrompt);
                standardFlashCardRepository.save(standardFlashCardEntity);
                log.info("Flashcard padrão para o prompt '{}' salvo com sucesso no cache.", standardizedPrompt);
            } else {
                log.warn("Tentativa de salvar prompt duplicado no cache: '{}'. Operação ignorada.", standardizedPrompt);
            }
        } else {
            log.warn("Não foi possível salvar o flashcard padrão devido a dados nulos. Prompt: {}", prompt);
        }
    }

    @Async
    public CompletableFuture<FlashcardResponseDTO> createResponseFromStandardCard(StandardFlashCardEntity card) {
        CompletableFuture<String> imageBase64Future = CompletableFuture.supplyAsync(() -> {
            try {
                if (card.getImagePath() == null || card.getImagePath().isBlank()) return null;
                Path imagePath = storageBasePath.resolve(card.getImagePath().substring("/storage/".length()));
                if (Files.exists(imagePath)) {
                    byte[] imageBytes = Files.readAllBytes(imagePath);
                    return Base64.getEncoder().encodeToString(imageBytes);
                }
                log.warn("Arquivo de imagem do cache não encontrado: {}", imagePath);
                return null;
            } catch (IOException e) {
                log.error("Erro ao ler arquivo de imagem do cache.", e);
                return null;
            }
        });

        CompletableFuture<String> audioBase64Future = CompletableFuture.supplyAsync(() -> {
            try {
                if (card.getAudioPath() == null || card.getAudioPath().isBlank()) return null;
                Path audioPath = storageBasePath.resolve(card.getAudioPath().substring("/storage/".length()));
                if (Files.exists(audioPath)) {
                    byte[] audioBytes = Files.readAllBytes(audioPath);
                    return Base64.getEncoder().encodeToString(audioBytes);
                }
                log.warn("Arquivo de áudio do cache não encontrado: {}", audioPath);
                return null;
            } catch (IOException e) {
                log.error("Erro ao ler arquivo de áudio do cache.", e);
                return null;
            }
        });

        return imageBase64Future.thenCombine(audioBase64Future, (imageBase64, audioBase64) ->
                new FlashcardResponseDTO(
                        card.getFront(),
                        card.getBack(),
                        imageBase64,
                        audioBase64
                )
        );
    }


    public UserEntity getUsuarioLogado() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email) // Assumindo que findByUsername agora é findByEmail
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado: " + email));
    }


}
