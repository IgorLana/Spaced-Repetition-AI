package com.spaced_repetition_ai.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class Deck {
    private Long id;
    private String name;
    private String description;
    private LocalDateTime createdDate;
    private Language TargetLanguage;
    private Language SourceLanguage;
    private String AudioPath;
    private String ImagePath;
    private double easeFactor;
    private boolean generateImage;
    private boolean generateAudio;
    private DeckType deckType;
    private ImageStyle imageStyle;
}
