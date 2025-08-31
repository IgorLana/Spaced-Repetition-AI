package com.spaced_repetition_ai.payment.dto;

import jakarta.validation.constraints.NotBlank;

public record PurchaseCreditsRequestDTO(
        @NotBlank(message = "O ID do pacote de créditos não pode ser vazio.")
        String packageId // Ex: "package_300", "package_600", "package_1000"
) {
}
