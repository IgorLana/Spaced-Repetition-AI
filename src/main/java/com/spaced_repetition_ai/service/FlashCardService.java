package com.spaced_repetition_ai.service;

import com.spaced_repetition_ai.dto.FlashcardRequestDTO;
import com.spaced_repetition_ai.dto.FlashcardResponseDTO;
import com.spaced_repetition_ai.entity.DeckEntity;
import com.spaced_repetition_ai.entity.FlashCardEntity;
import com.spaced_repetition_ai.entity.UserEntity;
import com.spaced_repetition_ai.exception.DatabaseException;
import com.spaced_repetition_ai.exception.ExternalServiceException;
import com.spaced_repetition_ai.exception.NotFoundException;
import com.spaced_repetition_ai.model.DeckType;
import com.spaced_repetition_ai.model.FlashCard;
import com.spaced_repetition_ai.model.ReviewRating;
import com.spaced_repetition_ai.repository.DeckRepository;
import com.spaced_repetition_ai.repository.FlashCardRepository;
import com.spaced_repetition_ai.repository.UserRepository;
import org.slf4j.Logger;

import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
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

    public FlashCardService(TextGenerationService textGenerationService, ImageGenerationService imageGenerationService,
                            AudioGenerationService audioGenerationService, FlashCardRepository flashCardRepository,
                            DeckRepository deckRepository, UserRepository userRepository) {
        this.textGenerationService = textGenerationService;
        this.imageGenerationService = imageGenerationService;
        this.audioGenerationService = audioGenerationService;
        this.flashCardRepository = flashCardRepository;
        this.deckRepository = deckRepository;
        this.userRepository = userRepository;
    }

    public List<FlashcardResponseDTO> listFlashCardsByDeck(Long deckId) {

        UserEntity usuarioLogado = getUsuarioLogado();

        try {
            List<FlashCardEntity> flashCards = flashCardRepository.findByDeckIdAndDeckUserId(deckId, getUsuarioLogado().getId());

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

        FlashCardEntity flashCardEntity = flashCardRepository.findByIdAndDeckUserId(flashCardId, getUsuarioLogado().getId())
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


        FlashCardEntity ent = flashCardRepository.findByIdAndDeckUserId(id, getUsuarioLogado().getId())
                        .orElseThrow(() -> new NotFoundException("FlashCard não encontrado com id: " + id));


            ent.setFront(dto.getFront());
            ent.setBack(dto.getBack());
            ent.setImagePath(dto.getImagePath());
            ent.setAudioPath(dto.getAudioPath());

            try {
                flashCardRepository.save(ent);
                log.info("FlashCard atualizado com sucesso!");
            }catch (Exception e){
                throw new DatabaseException("Erro ao atualizar o flashcard", e);
            }
    }

    public void generateFlashCard(Long deckId, FlashcardRequestDTO dto) {

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

        String front;
        String back;

        try {
            if (deckEntity.getDeckType() == DeckType.LANGUAGE) {

                FlashCard card = textGenerationService.generateTextFromJson(
                        deckEntity.getStandardTextPrompt() + deckEntity.getTextPrompt() + "Comece agora com a palavra: " + prompt
                                + "A lingua nativa é:" + deckEntity.getSourceLanguage().getLocaleCode() + ". E a lingua alvo para aprender é:" + deckEntity.getTargetLanguage().getLocaleCode());
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

        if (deckEntity.getGenerateImage()) {

            try {
                imagePath = imageGenerationService.generateImage(deckEntity.getImagePrompt() + prompt, null).get(0);
            } catch (Exception e) {
                throw new ExternalServiceException("Erro ao gerar a imagem do flashcard via AI", e);
            }
        }

        if (deckEntity.getGenerateAudio()) {
            try {
                audioPath = audioGenerationService.generateAudio(deckEntity.getAudioPrompt() + front, null).get(0);
            } catch (Exception e) {
                throw new ExternalServiceException("Erro ao gerar o audio do flashcard via AI", e);
            }
        }

        LocalDateTime createdDate = LocalDateTime.now();
        LocalDateTime lastReview = LocalDateTime.now();
        LocalDateTime nextReview = createdDate.plusMinutes(1);
        int interval = 1;

        double easeFactor = deckEntity.getEaseFactor();

        FlashCardEntity flashCardEntity = new FlashCardEntity(null, front, back, imagePath, audioPath,createdDate, lastReview, nextReview, interval, ReviewRating.BOM, easeFactor, deckEntity);
        try {
            flashCardRepository.save(flashCardEntity);
        } catch (Exception e) {
            throw new DatabaseException("Erro ao salvar o flashcard via AI", e);
        }

        log.info("FlashCard gerado com sucesso!");

        return new FlashcardResponseDTO(
                null, front, back,
                imagePath, audioPath, createdDate, lastReview,
                nextReview, interval, ReviewRating.BOM,
                easeFactor, deckId
        );
    }

    public void saveStandardFlashCards(String prompt, String front, String back, String imagePath, String audioPath) {}


    private UserEntity getUsuarioLogado() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username) // Assumindo que findByUsername agora é findByEmail
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado: " + username));
    }


}
