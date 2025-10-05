package com.spaced_repetition_ai.service;

import com.spaced_repetition_ai.dto.FlashcardRequestDTO;
import com.spaced_repetition_ai.dto.FlashcardResponseDTO;
import com.spaced_repetition_ai.entity.DeckEntity;
import com.spaced_repetition_ai.entity.FlashCardEntity;
import com.spaced_repetition_ai.entity.StandardFlashCardEntity;
import com.spaced_repetition_ai.entity.UserEntity;
import com.spaced_repetition_ai.exception.DatabaseException;
import com.spaced_repetition_ai.exception.NotFoundException;
import com.spaced_repetition_ai.model.*;
import com.spaced_repetition_ai.repository.DeckRepository;
import com.spaced_repetition_ai.repository.FlashCardRepository;
import com.spaced_repetition_ai.repository.StandardFlashCardRepository;
import com.spaced_repetition_ai.repository.UserRepository;
import org.slf4j.Logger;

import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;
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
    private final AwsService awsService;

    public FlashCardService(TextGenerationService textGenerationService, ImageGenerationService imageGenerationService,
                            AudioGenerationService audioGenerationService, FlashCardRepository flashCardRepository,
                            DeckRepository deckRepository, UserRepository userRepository,
                            StandardFlashCardRepository standardFlashCardRepository,
                            AwsService awsService) {
        this.textGenerationService = textGenerationService;
        this.imageGenerationService = imageGenerationService;
        this.audioGenerationService = audioGenerationService;
        this.flashCardRepository = flashCardRepository;
        this.deckRepository = deckRepository;
        this.userRepository = userRepository;
        this.standardFlashCardRepository = standardFlashCardRepository;
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

    public FlashcardResponseDTO getFlashCardById(Long flashCardId) {
        UserEntity usuarioLogado = getUsuarioLogado();

        FlashCardEntity flashCard = flashCardRepository.findByIdAndDeckUserId(flashCardId, usuarioLogado.getId())
                .orElseThrow(() -> new NotFoundException("Flash card não encontrado com id: " + flashCardId));

        return new FlashcardResponseDTO(flashCard.getFront(), flashCard.getBack(), flashCard.getImagePath(), flashCard.getAudioPath());

    }

    public void updateFlashCard(Long id, FlashcardRequestDTO dto) {
        UserEntity usuarioLogado = getUsuarioLogado();

        FlashCardEntity flashCard = flashCardRepository.findByIdAndDeckUserId(id, usuarioLogado.getId())
                .orElseThrow(() -> new NotFoundException("FlashCard não encontrado com id: " + id));

        boolean frontVazio = dto.getFront() == null || dto.getFront().trim().isEmpty();
        boolean backVazio = dto.getBack() == null || dto.getBack().trim().isEmpty();

        if (frontVazio && backVazio) {
            throw new IllegalArgumentException("Pelo menos um dos campos (front ou back) deve ser preenchido.");
        }

        if (!frontVazio) {
            flashCard.setFront(dto.getFront());
        }

        if (!backVazio) {
            flashCard.setBack(dto.getBack());
        }

        flashCard.setImagePath(dto.getImagePath());
        flashCard.setAudioPath(dto.getAudioPath());

        try {
            flashCardRepository.save(flashCard);
            log.info("FlashCard atualizado com sucesso!");
        } catch (Exception e) {
            throw new DatabaseException("Erro ao atualizar o flashcard", e);
        }
    }

    public void generateFlashCard(Long deckId, FlashcardRequestDTO dto) {

        UserEntity usuarioLogado = getUsuarioLogado();
        DeckEntity deckEntity = deckRepository.findByUserIdAndId(usuarioLogado.getId(), deckId)
                .orElseThrow(() -> new IllegalArgumentException("Deck não encontrado ou não pertence ao usuário."));

        LocalDateTime createdDate = LocalDateTime.now();
        LocalDateTime lastReview = LocalDateTime.now();
        LocalDateTime nextReview = createdDate.plusMinutes(1);

        if (dto.getFront() == null || dto.getFront().trim().isEmpty()) {
            throw new IllegalArgumentException("A frente do flashcard não pose ser vazia.");
        }

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

    private record MediaData(String base64) {}


    @Transactional
    @Async
    public CompletableFuture<FlashcardResponseDTO> generateAiFlashCard(String prompt, Long deckId) throws IOException {

        UserEntity usuarioLogado = getUsuarioLogado();
        DeckEntity deckEntity = deckRepository.findByUserIdAndId(usuarioLogado.getId(), deckId)
                .orElseThrow(() -> new RuntimeException("Deck não encontrado ou não pertence ao usuário."));

        if(prompt == null || prompt.trim().isEmpty()){
            throw new IllegalArgumentException("O prompt do flashcard não pode ser vazio.");
        }

        final String standardizedPrompt = prompt.toLowerCase().trim();
        Optional<StandardFlashCardEntity> cachedCard = standardFlashCardRepository.findByPromptAndImageStyleAndSourceLanguageAndTargetLanguage(standardizedPrompt, deckEntity.getImageStyle(), deckEntity.getSourceLanguage(), deckEntity.getTargetLanguage());
        if (cachedCard.isPresent()) {
            log.info("Cache hit para o prompt: {}. Retornando flashcard padrão.", standardizedPrompt);
            return createResponseFromStandardCard(cachedCard.get());
        }

        final CompletableFuture<MediaData> imageFuture = (deckEntity.getGenerateImage() && usuarioLogado.getBalance() >= 5)
                ? imageGenerationService.generateImageAsync(prompt, null, deckEntity.getImageStyle(), usuarioLogado.getId())
                .thenApply(imageData -> {
                    if (imageData == null || imageData.imageBytes() == null) return new MediaData(null);
                    String base64 = Base64.getEncoder().encodeToString(imageData.imageBytes());
                    return new MediaData(base64);
                })
                : CompletableFuture.completedFuture(new MediaData(null));

        CompletableFuture<FlashCard> textFuture = textGenerationService.generateTextFromJsonAsync(
                deckEntity.getDeckType() == DeckType.LANGUAGE ?
                        (TextPromptStyle.Language.getTemplate() + "Comece agora com a palavra: " + prompt + ". A lingua nativa é: " + deckEntity.getSourceLanguage().getLocaleCode() + " . E a lingua alvo para aprender é: " + deckEntity.getTargetLanguage().getLocaleCode()) :
                        (TextPromptStyle.GeneralFlashcards.getTemplate() + "Comece agora com a palavra: " + prompt)
        );

        CompletableFuture<MediaData> audioFuture = textFuture.thenCompose(card -> {
            if (deckEntity.getGenerateAudio() && usuarioLogado.getBalance() >= 1 && card != null) {
                return audioGenerationService.generateAudioAsync(card.getFront(), null, usuarioLogado.getId())
                        .thenApply(audioData -> { // Recebe os bytes do áudio
                            if (audioData == null || audioData.audioBytes() == null) return new MediaData(null);
                            String base64 = Base64.getEncoder().encodeToString(audioData.audioBytes());
                            return new MediaData(base64);
                        });
            }
            return CompletableFuture.completedFuture(new MediaData(null));
        });

        return CompletableFuture.allOf(imageFuture, textFuture, audioFuture)
                .thenApply(v -> {
                    MediaData imageData = imageFuture.join();
                    FlashCard card = textFuture.join();
                    MediaData audioData = audioFuture.join();
                    ImageStyle imageStyle = deckEntity.getImageStyle();

                    if (card == null) {
                        throw new RuntimeException("Falha ao gerar o texto do flashcard.");
                    }

                    CompletableFuture.runAsync(() ->
                            saveStandardFlashCards(prompt, card.getFront(), card.getBack(), imageData.base64, audioData.base64, imageStyle, deckEntity.getTargetLanguage(), deckEntity.getSourceLanguage())
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

    public void saveStandardFlashCards(String prompt, String front, String back, String imageBase64, String audioBase64, ImageStyle imageStyle, Language targetLanguage, Language sourceLanguage) {
        if (prompt != null && !prompt.trim().isEmpty() && front != null && back != null && !back.trim().isEmpty() && audioBase64 != null && !audioBase64.isBlank() && imageBase64 != null && !imageBase64.isBlank()) {

            String standardizedPrompt = prompt.toLowerCase().trim();

            if(standardFlashCardRepository.findByPromptAndImageStyleAndSourceLanguageAndTargetLanguage(standardizedPrompt, imageStyle, targetLanguage, sourceLanguage).isPresent()) {
            log.warn("Flashcard Standard já está salvo no banco de dados");
                return;
            }

            String imagePath = null;
            String audioPath = null;

            try {
                    if (imageBase64 != null && !imageBase64.isBlank()) {
                        byte[] imageBytes = Base64.getDecoder().decode(imageBase64);
                        imagePath = awsService.uploadPublicMedia(imageBytes, "image", "jpg");
                        log.info("Imagem salva na pasta pública do S3: {}", imagePath);
                    }

                    if(audioBase64 != null && !audioBase64.isBlank()) {
                        byte[] audioBytes = Base64.getDecoder().decode(audioBase64);
                        audioPath = awsService.uploadPublicMedia(audioBytes, "audio", "mp3");
                        log.info("Áudio salvo na pasta pública do S3: {}", audioPath);
                    }

                    StandardFlashCardEntity standardFlashCardEntity = new StandardFlashCardEntity(null, front, back, imagePath, audioPath, standardizedPrompt, imageStyle, targetLanguage, sourceLanguage);
                    standardFlashCardRepository.save(standardFlashCardEntity);
                    log.info("Flashcard padrão salvo com sucesso!");

                } catch (Exception e) {
                log.error("Erro ao salvar flashcard");
            }

        }
    }


    @Async
    public CompletableFuture<FlashcardResponseDTO> createResponseFromStandardCard(StandardFlashCardEntity card) {

        CompletableFuture<String> imageBase64Future = CompletableFuture.supplyAsync(() -> {
            if (card.getImagePath() == null || card.getImagePath().isBlank()) {
                return null;
            }
            return awsService.downloadFileAsBase64(card.getImagePath());
        });

        CompletableFuture<String> audioBase64Future = CompletableFuture.supplyAsync(() -> {
            if (card.getAudioPath() == null || card.getAudioPath().isBlank()) {
                return null;
            }
            return awsService.downloadFileAsBase64(card.getAudioPath());
        });

        return imageBase64Future.thenCombine(audioBase64Future, (imageBase64, audioBase64) ->
                new FlashcardResponseDTO(
                        card.getFront(),
                        card.getBack(),
                        imageBase64,  // Retorna base64
                        audioBase64   // Retorna base64
                )
        );
    }





    public UserEntity getUsuarioLogado() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email) // Assumindo que findByUsername agora é findByEmail
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado: " + email));
    }


}
