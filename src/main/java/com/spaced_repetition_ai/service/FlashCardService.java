package com.spaced_repetition_ai.service;

import com.spaced_repetition_ai.dto.FlashcardRequestDTO;
import com.spaced_repetition_ai.dto.FlashcardResponseDTO;
import com.spaced_repetition_ai.entity.DeckEntity;
import com.spaced_repetition_ai.entity.FlashCardEntity;
import com.spaced_repetition_ai.entity.StandardFlashCardEntity;
import com.spaced_repetition_ai.entity.UserEntity;
import com.spaced_repetition_ai.exception.DatabaseException;
import com.spaced_repetition_ai.exception.ExternalServiceException;
import com.spaced_repetition_ai.exception.NotFoundException;
import com.spaced_repetition_ai.model.DeckType;
import com.spaced_repetition_ai.model.FlashCard;
import com.spaced_repetition_ai.model.ReviewRating;
import com.spaced_repetition_ai.repository.DeckRepository;
import com.spaced_repetition_ai.repository.FlashCardRepository;
import com.spaced_repetition_ai.repository.StandardFlashCardRepository;
import com.spaced_repetition_ai.repository.UserRepository;
import com.spaced_repetition_ai.util.DefaultPrompts;
import org.slf4j.Logger;

import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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

    public FlashCardService(TextGenerationService textGenerationService, ImageGenerationService imageGenerationService,
                            AudioGenerationService audioGenerationService, FlashCardRepository flashCardRepository,
                            DeckRepository deckRepository, UserRepository userRepository, StandardFlashCardRepository standardFlashCardRepository) {
        this.textGenerationService = textGenerationService;
        this.imageGenerationService = imageGenerationService;
        this.audioGenerationService = audioGenerationService;
        this.flashCardRepository = flashCardRepository;
        this.deckRepository = deckRepository;
        this.userRepository = userRepository;
        this.standardFlashCardRepository = standardFlashCardRepository;
    }

    public List<FlashcardResponseDTO> listFlashCardsByDeck(Long deckId) {

        UserEntity usuarioLogado = getUsuarioLogado();

        try {
            List<FlashCardEntity> flashCards = flashCardRepository.findByDeckIdAndDeckUserId(deckId, usuarioLogado.getId());

            return flashCards.stream()
                    .map(FlashcardResponseDTO::flashFromEntity)
                    .collect(Collectors.toList());
        }catch (Exception e){
            log.error("Erro ao listar os flashcards", e);
            throw new DatabaseException("Erro ao listar os flashcards", e);

        }
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

        double easeFactor = deckEntity.getEaseFactor();

        int interval = 1;

        FlashCardEntity flashCardEntity = new FlashCardEntity(
                null, dto.getFront(), dto.getBack(), dto.getImagePath(),
                dto.getAudioPath(), createdDate,lastReview, nextReview,
                interval, ReviewRating.BOM, easeFactor, deckEntity
        );

        try {
            flashCardRepository.save(flashCardEntity);
        }catch (Exception e){
            throw new DatabaseException("Erro ao salvar o flashcard", e);
        }
        log.info("FlashCard salvo com sucesso!");

    }

    public FlashcardResponseDTO generateAiFlashCard(String prompt, Long deckId) {

        UserEntity usuarioLogado = getUsuarioLogado();
        DeckEntity deckEntity = deckRepository.findByUserIdAndId(usuarioLogado.getId(), deckId)
                .orElseThrow(() -> new RuntimeException("Deck não encontrado ou não pertence ao usuário."));

        if(prompt == null || prompt.trim().isEmpty()){
            throw new IllegalArgumentException("O prompt do flashcard não pose ser vazia.");
        }

        String front;
        String back;
        LocalDateTime createdDate = LocalDateTime.now();
        LocalDateTime lastReview = LocalDateTime.now();
        LocalDateTime nextReview = createdDate.plusMinutes(1);
        int interval = 1;
        double easeFactor = deckEntity.getEaseFactor();

        // Verifica se o deck é standard
        boolean standardDeck = DefaultPrompts.DEFAULT_TEXT_PROMPT_LANGUAGE.equals(deckEntity.getTextPrompt()) &&
                DefaultPrompts.DEFAULT_IMAGE_PROMPT.equals(deckEntity.getImagePrompt()) &&
                deckEntity.getAudioPrompt().isEmpty();

        // Verifica se já existe um flashcard standard
        Optional<StandardFlashCardEntity> flashcardOptional = standardFlashCardRepository.findByPrompt(prompt);

        if (standardDeck && flashcardOptional.isPresent() ) {
            FlashCardEntity flashCardEntity = new FlashCardEntity(null, flashcardOptional.get().getFront(), flashcardOptional.get().getBack(),
                    flashcardOptional.get().getImagePath(), flashcardOptional.get().getAudioPath(),createdDate,
                    lastReview, nextReview, interval, ReviewRating.BOM, easeFactor, deckEntity);

            return new FlashcardResponseDTO(
                    null, flashcardOptional.get().getFront(), flashcardOptional.get().getBack(),
                    flashcardOptional.get().getImagePath(), flashcardOptional.get().getAudioPath(), createdDate, lastReview,
                    nextReview, interval, ReviewRating.BOM,
                    easeFactor, deckId
            );
        }

        try {
            if (deckEntity.getDeckType() == DeckType.LANGUAGE) {

                FlashCard card = textGenerationService.generateTextFromJson(
                        deckEntity.getStandardTextPrompt() + deckEntity.getTextPrompt() + "Comece agora com a palavra: " + prompt
                                + "A lingua nativa é: " + deckEntity.getSourceLanguage().getLocaleCode() + ". E a lingua alvo para aprender é: " + deckEntity.getTargetLanguage().getLocaleCode());
                front = card.getFront();
                back = card.getBack();
            } else {
                FlashCard card = textGenerationService.generateTextFromJson(
                        deckEntity.getStandardTextPrompt() + deckEntity.getTextPrompt() + "Comece agora com a palavra: " + prompt);
                front = card.getFront();
                back = card.getBack();
            }

        } catch (Exception e) {
            throw new ExternalServiceException("Erro ao gerar o texto do flashcard via AI", e);
        }
        String audioPath = null;
        String imagePath = null;

        //Verificação de saldo
        if(usuarioLogado.getBalance() < 6 && deckEntity.getGenerateImage() & deckEntity.getGenerateAudio()){
                FlashCardEntity flashCardEntity = new FlashCardEntity(null, front, back, imagePath, audioPath,createdDate, lastReview, nextReview, interval, ReviewRating.BOM, easeFactor, deckEntity);
                try {
                    flashCardRepository.save(flashCardEntity);
                } catch (Exception e) {
                    throw new DatabaseException("Erro ao salvar o flashcard via AI", e);
                }

                log.info("FlashCard gerado com sucesso! Não foi possivel gerar imagem nem audio, devido ao saldo insuficiente.");

                if (standardDeck) {
                    saveStandardFlashCards(prompt, front, back, imagePath, audioPath);
                }

                return new FlashcardResponseDTO(
                        null, front, back,
                        imagePath, audioPath, createdDate, lastReview,
                        nextReview, interval, ReviewRating.BOM,
                        easeFactor, deckId);
        }

        if (deckEntity.getGenerateImage()) {
            if(usuarioLogado.getBalance() >= 5) {
                try {
                    List<String> imagePaths = imageGenerationService.generateImage(deckEntity.getImagePrompt() + prompt, null);
                    if (imagePaths != null && !imagePaths.isEmpty()) {
                        imagePath = imagePaths.get(0);
                    }else{
                        log.info("Erro ao gerar audio.");
                    }
                } catch (Exception e) {
                    throw new ExternalServiceException("Erro ao gerar a imagem do flashcard via IA", e);
                }
            }else {
                log.info("Saldo insuficiente para gerar Imagem.");
            }
        }

        if (deckEntity.getGenerateAudio()) {
            if(usuarioLogado.getBalance() >= 1) {
                try {

                    List<String> audioPaths = audioGenerationService.generateAudio(deckEntity.getAudioPrompt() + front, null);
                    if (audioPaths != null && !audioPaths.isEmpty()) {
                        audioPath = audioPaths.get(0);
                    }else{
                        log.info("Erro ao gerar audio.");
                    }
                } catch (Exception e) {
                    throw new ExternalServiceException("Erro ao gerar o audio do flashcard via IA", e);
                }
            }else {
                log.info("Saldo insuficiente para gerar audio.");
            }
        }

        FlashCardEntity flashCardEntity = new FlashCardEntity(null, front, back, imagePath, audioPath,createdDate, lastReview, nextReview, interval, ReviewRating.BOM, easeFactor, deckEntity);
        try {
            flashCardRepository.save(flashCardEntity);
        } catch (Exception e) {
            throw new DatabaseException("Erro ao salvar o flashcard via IA", e);
        }

        log.info("FlashCard gerado com sucesso!");

        if (standardDeck) {
            saveStandardFlashCards(prompt, front, back, imagePath, audioPath);
        }

        return new FlashcardResponseDTO(
                null, front, back,
                imagePath, audioPath, createdDate, lastReview,
                nextReview, interval, ReviewRating.BOM,
                easeFactor, deckId
        );
    }

    public void saveStandardFlashCards(String prompt, String front, String back, String imagePath, String audioPath) {
        if(prompt != null && !prompt.trim().isEmpty() && imagePath != null  && audioPath != null){
            StandardFlashCardEntity standardFlashCardEntity = new StandardFlashCardEntity(null, front, back, imagePath, audioPath, prompt);
            standardFlashCardRepository.save(standardFlashCardEntity);
        }
    }


    private UserEntity getUsuarioLogado() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email) // Assumindo que findByUsername agora é findByEmail
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado: " + email));
    }


}
