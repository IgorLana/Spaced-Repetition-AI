package com.spaced_repetition_ai.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;


@Data
@NoArgsConstructor
public class Review {
    private Long id;
    private String deckId;
    private String flashCardId;
    private ReviewRating rating;
    private LocalDateTime ReviewDate;
    private LocalDateTime NextReviewDate;
    private int interval;
    private double easeFactor;
    private long timeSpent;
}
