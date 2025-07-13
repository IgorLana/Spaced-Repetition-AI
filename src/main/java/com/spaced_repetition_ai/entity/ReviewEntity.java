package com.spaced_repetition_ai.entity;


import com.spaced_repetition_ai.model.ReviewRating;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "Review")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReviewEntity {

    @Id
    private String id;
    @ManyToOne
    private String deckId;
    private String flashCardId;
    private ReviewRating rating;
    private LocalDateTime ReviewDate;
    private LocalDateTime NextReviewDate;
    private int interval;
    private double easeFactor;
    private long timeSpent;


}
