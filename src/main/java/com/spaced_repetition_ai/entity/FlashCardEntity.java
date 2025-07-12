package com.spaced_repetition_ai.entity;

import com.spaced_repetition_ai.model.ReviewRating;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "FlashCard")
@Data
@NoArgsConstructor
public class FlashCardEntity {

    @Id
    private String id;
    private String front;
    private String back;
    private String imagePath;
    private String audioPath;
    private LocalDateTime createdDate;
    private LocalDateTime lastReview;
    private LocalDateTime nextReview;
    private int interval;
    private double easeFactor;
    private ReviewRating rating;

    public FlashCardEntity(String id, String front, String back, String imagePath,
                           String audioPath, LocalDateTime createdDate, LocalDateTime lastReview,
                           LocalDateTime nextReview, int interval, ReviewRating rating, double easeFactor) {
        this.id = null;
        this.front = front;
        this.back = back;
        this.imagePath = imagePath;
        this.audioPath = audioPath;
        this.createdDate = createdDate;
        this.lastReview = lastReview;
        this.nextReview = nextReview;
        this.interval = interval;
        this.rating = rating;
        this.easeFactor = easeFactor;
    }

}
