package com.spaced_repetition_ai.dto;

import com.spaced_repetition_ai.model.DeckType;
import com.spaced_repetition_ai.model.Language;

public record DeckDetailsReponseDTO(
        Long id,
        String name,
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
