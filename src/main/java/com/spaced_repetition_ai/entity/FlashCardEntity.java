package com.spaced_repetition_ai.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.spaced_repetition_ai.model.ReviewRating;
import jakarta.persistence.*;

import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.persistence.Id;

import java.time.LocalDateTime;


@Entity
@Table(name = "flashcards")
@Data
@NoArgsConstructor
public class FlashCardEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
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


    @ManyToOne(fetch = FetchType.LAZY) // Define a relação: Muitos flashcards para UM deck.
    @JoinColumn(name = "deck_id", nullable = false)
    @JsonBackReference
    private DeckEntity deck;

    public FlashCardEntity(Long id, String front, String back, String imagePath,
                           String audioPath, LocalDateTime createdDate, LocalDateTime lastReview,
                           LocalDateTime nextReview, int interval, ReviewRating rating, double easeFactor, DeckEntity deck) {
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
        this.deck = deck;

    }

}
