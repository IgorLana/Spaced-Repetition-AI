package com.spaced_repetition_ai.payment.dto;

public record CreatePreferenceResponseDTO(
        String preferenceId,
        String redirectUrl // The Mercado Pago init_point URL
) {}
