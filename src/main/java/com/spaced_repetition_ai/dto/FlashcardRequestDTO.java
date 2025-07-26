package com.spaced_repetition_ai.dto;


import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.lang.Nullable;


@Data
@NoArgsConstructor
public class FlashcardRequestDTO{
        String front;
        String back;

        @Nullable
        String imagePath;

        @Nullable
        String audioPath;

}
