package com.spaced_repetition_ai.dto;

public record AuthRequestDTO(
        String username,
        String password,
        String email
) {
}
