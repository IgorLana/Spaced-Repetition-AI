package com.spaced_repetition_ai.dto;

import java.util.Map;

public record AuthResponse(
        String token,
        Map<String, String> cloudfrontSignedCookie
) {
}
