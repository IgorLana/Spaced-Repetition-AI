package com.spaced_repetition_ai.dto;


import org.springframework.lang.Nullable;

public record FlashcardRequestDTO(
        String front,
        String back,

        @Nullable
        String imagePath,

        @Nullable
        String audioPath
){
}
