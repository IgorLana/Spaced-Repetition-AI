package com.spaced_repetition_ai.dto;

public record AuthRequestDTO(
        String password,
        String email
) {
}
