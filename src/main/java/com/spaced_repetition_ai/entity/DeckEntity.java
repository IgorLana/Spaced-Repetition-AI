package com.spaced_repetition_ai.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "Deck")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeckEntity {

    @Id
    private String id;
    private String name;
    private String description;
    private String targetLanguage;
    private String sourceLanguage;
    private String audioPrompt;
    private String imagePrompt;
    private String textPrompt;
    private String audioPath;
    private String imagePath;
    private double easeFactor;


}
