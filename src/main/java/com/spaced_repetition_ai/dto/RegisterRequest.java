package com.spaced_repetition_ai.dto;

public record RegisterRequest(
        String name,
        String password,
        String email
) {
}
