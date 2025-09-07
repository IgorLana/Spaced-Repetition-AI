package com.spaced_repetition_ai.dto;

import org.springframework.lang.Nullable;

public record FlashcardResponseDTO (

        String frontText,
        String backText,
        @Nullable String imageBase64,
        @Nullable String audioBase64
) {

}
