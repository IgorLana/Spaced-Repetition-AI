package com.spaced_repetition_ai.dto;

import com.spaced_repetition_ai.entity.FlashCardEntity;
import com.spaced_repetition_ai.model.ReviewRating;

import java.time.LocalDateTime;

public record FlashcardResponseDTO (
        Long id,
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
        Long deckId
) {
    public static FlashcardResponseDTO flashFromEntity(FlashCardEntity flashCardEntity) {
        return new FlashcardResponseDTO(
                flashCardEntity.getId(),
                flashCardEntity.getFront(),
                flashCardEntity.getBack(),
                flashCardEntity.getImagePath(),
                flashCardEntity.getAudioPath(),
                flashCardEntity.getCreatedDate(),
                flashCardEntity.getLastReview(),
                flashCardEntity.getNextReview(),
                flashCardEntity.getInterval(),
                flashCardEntity.getRating(),
                flashCardEntity.getEaseFactor(),
                flashCardEntity.getDeck().getId()
        );
    }

}
