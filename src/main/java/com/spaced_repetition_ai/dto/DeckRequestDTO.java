package com.spaced_repetition_ai.dto;


import com.spaced_repetition_ai.model.DeckType;
import com.spaced_repetition_ai.model.ImageStyle;
import com.spaced_repetition_ai.model.Language;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;



@Data
@NoArgsConstructor
public class DeckRequestDTO {

    @NotBlank
    private String name;
    @NotBlank
    private String description;
    private Language targetLanguage;
    private Language sourceLanguage;
    private String audioPath;
    private String imagePath;
    private Double easeFactor;
    private Boolean generateImage;
    private Boolean generateAudio;
    private DeckType deckType;
    private ImageStyle imageStyle;

}
