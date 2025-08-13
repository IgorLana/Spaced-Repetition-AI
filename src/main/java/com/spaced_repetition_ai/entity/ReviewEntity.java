package com.spaced_repetition_ai.entity;


import com.spaced_repetition_ai.model.ReviewRating;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.persistence.Id;

import java.time.LocalDateTime;

@Entity
@Table(name = "reviews")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReviewEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long deckId;
    private Long flashCardId;
    private ReviewRating rating;
    private LocalDateTime ReviewDate;
    private LocalDateTime NextReviewDate;
    private int interval;
    private double easeFactor;
    private long timeSpent;


}
