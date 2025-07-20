package com.spaced_repetition_ai.dto;

import com.spaced_repetition_ai.model.Language;

public record DeckResponseDTO(

        String id,
        String name,
        String description,
        Language targetLanguage,
        Language sourceLanguage,
        String audioPrompt,
        String imagePrompt,
        String textPrompt,
        String audioPath,
        String imagePath,
        double easeFactor,
        String standardTextPrompt,
        String standardAudioPrompt,
        String standardImagePrompt
) {
}
