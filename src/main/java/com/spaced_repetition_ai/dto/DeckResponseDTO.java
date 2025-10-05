package com.spaced_repetition_ai.dto;

import com.spaced_repetition_ai.entity.DeckEntity;
import com.spaced_repetition_ai.model.DeckType;
import com.spaced_repetition_ai.model.ImageStyle;
import com.spaced_repetition_ai.model.Language;

public record DeckResponseDTO(
        Long id,
        String name,
        String description,
        Language targetLanguage,
        Language sourceLanguage,
        String audioPath,
        String imagePath,
        Double easeFactor,
        Boolean generateImage,
        Boolean generateAudio,
        DeckType deckType,
        ImageStyle imageStyle

        
) {
    public static DeckResponseDTO fromEntity(DeckEntity deckEntity) {
        return new DeckResponseDTO(

                deckEntity.getId(),
                deckEntity.getName(),
                deckEntity.getDescription(),
                deckEntity.getTargetLanguage(),
                deckEntity.getSourceLanguage(),
                deckEntity.getAudioPath(),
                deckEntity.getImagePath(),
                deckEntity.getEaseFactor(),
                deckEntity.getGenerateImage(),
                deckEntity.getGenerateAudio(),
                deckEntity.getDeckType(),
                deckEntity.getImageStyle()
                
        );
    }
}
