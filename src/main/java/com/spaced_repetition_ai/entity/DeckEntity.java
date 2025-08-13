package com.spaced_repetition_ai.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.spaced_repetition_ai.model.DeckType;
import com.spaced_repetition_ai.model.Language;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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
    @Column(columnDefinition = "TEXT")
    private String audioPrompt;
    @Column(columnDefinition = "TEXT")
    private String imagePrompt;
    @Column(columnDefinition = "TEXT")
    private String textPrompt;
    private String audioPath;
    private String imagePath;
    private double easeFactor;
    @Column(columnDefinition = "TEXT")
    private String standardTextPrompt;
    private boolean generateImage;
    private boolean generateAudio;
    private DeckType deckType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false) // Diz qual coluna na tabela 'decks' é a chave estrangeira para a tabela 'users'
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
