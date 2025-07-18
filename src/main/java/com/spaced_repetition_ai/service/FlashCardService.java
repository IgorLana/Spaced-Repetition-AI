package com.spaced_repetition_ai.service;

import com.spaced_repetition_ai.entity.DeckEntity;
import com.spaced_repetition_ai.entity.FlashCardEntity;
import com.spaced_repetition_ai.model.FlashCard;
import com.spaced_repetition_ai.model.ReviewRating;
import com.spaced_repetition_ai.repository.DeckRepository;
import com.spaced_repetition_ai.repository.FlashCardRepository;
import com.spaced_repetition_ai.repository.ReviewRepository;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class FlashCardService {

    private final TextGenerationService textGenerationService;
    private final ImageGenerationService imageGenerationService;
    private final AudioGenerationService audioGenerationService;
    private final FlashCardRepository flashCardRepository;
    private final DeckRepository deckRepository;
    private final ReviewRepository reviewRepository;

    public record GeneratedFlashCard(
            String frontText,
            String backText,
            String imagePath,
            String audioPath,
            LocalDateTime createdDate,
            LocalDateTime lastReview,
            LocalDateTime nextReview,
            double interval,
            ReviewRating review,
            double easeFactor,
            String deckId
    ) {}

    public FlashCardService(TextGenerationService textGenerationService, ImageGenerationService imageGenerationService,
                            AudioGenerationService audioGenerationService, FlashCardRepository flashCardRepository,
                            DeckRepository deckRepository, ReviewRepository reviewRepository) {
        this.textGenerationService = textGenerationService;
        this.imageGenerationService = imageGenerationService;
        this.audioGenerationService = audioGenerationService;
        this.flashCardRepository = flashCardRepository;
        this.deckRepository = deckRepository;
        this.reviewRepository = reviewRepository;
    }

    public void generateFlashCard(String front, String back, @Nullable String imagePath, @Nullable String audioPath) {

        LocalDateTime createdDate = LocalDateTime.now();
        LocalDateTime lastReview = createdDate;
        LocalDateTime nextReview = createdDate.plusMinutes(1);
        double easeFactor = 2.5;
        int interval = 1;
        FlashCardEntity flashCardEntity = new FlashCardEntity(null, front, back, imagePath, audioPath, createdDate,lastReview, nextReview, interval, ReviewRating.BOM, easeFactor, null);
        flashCardRepository.save(flashCardEntity);
        System.out.println("FlashCard salvo com sucesso!");
    }

    public GeneratedFlashCard generateAiFlashCard(String prompt, String deckId) {

        DeckEntity deckEntity = deckRepository.findById(deckId).orElseThrow();

        FlashCard card = textGenerationService.generateTextFromJson(
                deckEntity.getTextPrompt() + "Comece agora com a palavra: " + prompt
                        + "A lingua nativa é:" + deckEntity.getSourceLanguage() + "E a lingua alvo para aprender é:" + deckEntity.getTargetLanguage());
        String front = card.getFront();
        String back = card.getBack();
        String imagePath = imageGenerationService.generateImage(deckEntity.getImagePrompt() + prompt, null).get(0);
        String audioPath = audioGenerationService.generateAudio(deckEntity.getAudioPrompt() + front, null).get(0);
        LocalDateTime createdDate = LocalDateTime.now();
        LocalDateTime lastReview = createdDate;
        LocalDateTime nextReview = createdDate.plusMinutes(1);
        int interval = 1;
        double easeFactor = 2;

        FlashCardEntity flashCardEntity = new FlashCardEntity(null, front, back, imagePath, audioPath,createdDate, lastReview, nextReview, interval, ReviewRating.BOM, easeFactor, deckId);
        flashCardRepository.save(flashCardEntity);
        System.out.println("FlashCard salvo com sucesso!");
        return new GeneratedFlashCard(front, back, imagePath, audioPath, createdDate, lastReview, nextReview, interval, ReviewRating.BOM, easeFactor, deckId);
    }

}
