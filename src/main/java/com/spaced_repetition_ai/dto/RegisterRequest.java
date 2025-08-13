package com.spaced_repetition_ai.dto;

public record RegisterRequest(
        String username,
        String password,
        String email
) {
}
