package com.spaced_repetition_ai.dto;

import org.springframework.lang.Nullable;

public record FlashcardResponseDTO (

        String frontText,
        String backText,
        @Nullable String imageBase64,
        @Nullable String audioBase64
) {

    public String getFront() {
        return frontText;
    }
    public String getBack() {
        return backText;
    }
    public String getImage() {
        return imageBase64;
    }
    public String getAudio() {
        return audioBase64;
    }

}
