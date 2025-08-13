package com.spaced_repetition_ai.dto;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class ErrorResponseDTO {
    private String code;
    private String message;
    private LocalDateTime timestamp;

    public ErrorResponseDTO(String code, String message) {
    this.code = code;
    this.message = message;
    this.timestamp = LocalDateTime.now();
    }

}
