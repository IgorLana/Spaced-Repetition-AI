package com.spaced_repetition_ai.dto;

import com.spaced_repetition_ai.model.AuthProvider;

public record RegisterRequest(
        String name,
        String password,
        String email,
        AuthProvider authProvider
) {
}
