package com.spaced_repetition_ai.entity;

import com.spaced_repetition_ai.model.ImageStyle;
import com.spaced_repetition_ai.model.Language;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "standardFlashCards")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StandardFlashCardEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String front;
    private String back;
    private String imagePath;
    private String audioPath;
    private String prompt;
    private ImageStyle imageStyle;
    private Language targetLanguage;
    private Language sourceLanguage;

}
