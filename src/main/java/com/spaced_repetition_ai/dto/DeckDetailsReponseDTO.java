package com.spaced_repetition_ai.dto;

import com.spaced_repetition_ai.model.DeckType;
import com.spaced_repetition_ai.model.Language;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record DeckDetailsReponseDTO(
        Long id,
        @NotBlank
        String name,
        @NotNull
        String description,
        Language targetLanguage,
        Language sourceLanguage,
        Double easeFactor,
        Boolean generateImage,
        Boolean generateAudio,
        DeckType deckType,
        int flashcardsToReview,
        int totalFlashcardsDeck,
        double scoreDeck
) {
}
