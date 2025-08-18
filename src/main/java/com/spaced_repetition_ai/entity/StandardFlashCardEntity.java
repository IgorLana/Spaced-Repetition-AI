package com.spaced_repetition_ai.entity;

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

}
