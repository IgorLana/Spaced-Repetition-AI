package com.spaced_repetition_ai.payment.dto;

public record ProcessPaymentNotificationRequestDTO (
    String resourceType, // e.g., "payment"
    String resourceId    // e.g., the payment ID
) {}

