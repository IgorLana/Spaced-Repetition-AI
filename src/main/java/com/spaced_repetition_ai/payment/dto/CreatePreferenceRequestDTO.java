package com.spaced_repetition_ai.payment.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.List;

public record CreatePreferenceRequestDTO(
        Long userId,

        @DecimalMin(value = "0.01", message = "Total amount must be positive")
        BigDecimal totalAmount,

        @NotEmpty(message = "Items list cannot be empty")
        @Valid
        List<ItemDTO> items,

        @NotNull(message = "Payer information cannot be null")
        @Valid
        PayerDTO payer,

        @NotNull(message = "Back URLs cannot be null")
        @Valid
        BackUrlsDTO backUrls,

        // O endereço foi removido, mas o campo para a URL de notificação permanece.
        String notificationUrl
) {
    public record ItemDTO(
            @NotBlank(message = "Item ID cannot be blank")
            String id,
            @NotBlank(message = "Item title cannot be blank")
            String title,
            @NotNull(message = "Item quantity cannot be null")
            Integer quantity,
            @NotNull(message = "Item unit price cannot be null")
            @DecimalMin(value = "0.01", message = "Item unit price must be positive")
            BigDecimal unitPrice
    ) {}

    public record PayerDTO(
            @NotBlank(message = "Payer email cannot be blank")
            @Email(message = "Payer email must be valid")
            String email,
            @NotBlank(message = "Payer name cannot be blank")
            String name
    ) {}

    public record BackUrlsDTO(
            @NotBlank(message = "Success URL cannot be blank")
            String success,
            @NotBlank(message = "Failure URL cannot be blank")
            String failure,
            @NotBlank(message = "Pending URL cannot be blank")
            String pending
    ) {}

    // O DeliveryAddressDTO foi completamente removido daqui.
}