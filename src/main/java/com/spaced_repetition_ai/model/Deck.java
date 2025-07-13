package com.spaced_repetition_ai.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class Deck {
    private String id;
    private String name;
    private String description;
    private LocalDateTime createdDate;
    private String TargetLanguage;
    private String SourceLanguage;
    private String AudioPrompt;
    private String ImagePrompt;
    private String TextPrompt;
    private String AudioPath;
    private String ImagePath;
    private double easeFactor;
}
