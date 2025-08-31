package com.spaced_repetition_ai.dto;

import com.spaced_repetition_ai.entity.UserEntity;

public record UserResponseDTO(
        Long id,
        String name,
        String email,
        int balance
) {
    // Método de conveniência para converter a entidade em DTO
    public static UserResponseDTO fromEntity(UserEntity user) {
        return new UserResponseDTO(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getBalance()
        );
    }
}