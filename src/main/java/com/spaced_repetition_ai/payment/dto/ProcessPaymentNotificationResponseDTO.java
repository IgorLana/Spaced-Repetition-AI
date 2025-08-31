package com.spaced_repetition_ai.payment.dto;

public record ProcessPaymentNotificationResponseDTO(
        boolean success,
        String updatedStatus // e.g., "APPROVED", "REJECTED"
) {}
