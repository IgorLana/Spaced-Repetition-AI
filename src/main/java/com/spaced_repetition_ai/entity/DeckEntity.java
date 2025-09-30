package com.spaced_repetition_ai.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.spaced_repetition_ai.model.DeckType;
import com.spaced_repetition_ai.model.ImageStyle;
import com.spaced_repetition_ai.model.Language;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

import java.util.ArrayList;
import java.util.List;


@Entity
@Table(name = "decks")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeckEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    @Column(columnDefinition = "TEXT")
    private String description;
    private Language targetLanguage;
    private Language sourceLanguage;
    private String textPrompt;
    private String audioPath;
    private String imagePath;
    private double easeFactor;
    private boolean generateImage;
    private boolean generateAudio;
    private DeckType deckType;
    @ColumnDefault("0")
    private int totalReviewCount;
    @ColumnDefault("0")
    private int totalReviewRate;

    private ImageStyle imageStyle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @OneToMany(mappedBy = "deck", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<FlashCardEntity> flashCards = new ArrayList<>();

    public boolean getGenerateImage() {
        return generateImage;
    }

    public boolean getGenerateAudio() {
        return generateAudio;
    }
}
