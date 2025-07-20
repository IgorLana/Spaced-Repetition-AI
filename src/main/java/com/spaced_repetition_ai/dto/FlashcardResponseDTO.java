package com.spaced_repetition_ai.dto;

import com.spaced_repetition_ai.model.ReviewRating;

import java.time.LocalDateTime;

public record FlashcardResponseDTO (
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
) {

}
