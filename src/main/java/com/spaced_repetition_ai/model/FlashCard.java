package com.spaced_repetition_ai.model;


import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class FlashCard {

    private Long id;
    private String front;
    private String back;
    private String imagePath;
    private String audioPath;
    private LocalDateTime createdDate;
    private LocalDateTime lastReview;
    private LocalDateTime nextReview;
    private double interval;
    private Deck deck;


}
