package com.spaced_repetition_ai.dto;

import com.spaced_repetition_ai.entity.DeckEntity;
import com.spaced_repetition_ai.model.DeckType;
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
        Double easeFactor,
        Boolean generateImage,
        Boolean generateAudio,
        DeckType deckType,
        String standardTextPrompt
        
) {
    public static DeckResponseDTO fromEntity(DeckEntity deckEntity) {
        return new DeckResponseDTO(

                deckEntity.getId(),
                deckEntity.getName(),
                deckEntity.getDescription(),
                deckEntity.getTargetLanguage(),
                deckEntity.getSourceLanguage(),
                deckEntity.getAudioPrompt(),
                deckEntity.getImagePrompt(),
                deckEntity.getTextPrompt(),
                deckEntity.getAudioPath(),
                deckEntity.getImagePath(),
                deckEntity.getEaseFactor(),
                deckEntity.getGenerateImage(),
                deckEntity.getGenerateAudio(),
                deckEntity.getDeckType(),
                deckEntity.getStandardTextPrompt()
                
        );
    }
}
