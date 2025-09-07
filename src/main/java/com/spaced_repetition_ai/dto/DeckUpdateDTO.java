package com.spaced_repetition_ai.dto;


import com.spaced_repetition_ai.model.Language;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class DeckUpdateDTO {
    private String name;
    private String description;
    private String audioPath;
    private String imagePath;
    private Double easeFactor;
    private Boolean generateImage;
    private Boolean generateAudio;
    private Language targetLanguage;
    private Language sourceLanguage;


}
