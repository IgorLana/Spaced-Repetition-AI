package com.spaced_repetition_ai.dto;


import com.spaced_repetition_ai.model.Language;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class DeckUpdateDTO {
    private String name;
    private String description;
    private String audioPrompt;
    private String imagePrompt;
    private String textPrompt;
    private String audioPath;
    private String imagePath;
    private Double easeFactor;
    private Boolean generateImage;
    private Boolean generateAudio;
    private String standardTextPrompt;
    private String standardAudioPrompt;
    private String standardImagePrompt;
    private Language targetLanguage;
    private Language sourceLanguage;


}
