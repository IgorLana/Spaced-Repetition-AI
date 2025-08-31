package com.spaced_repetition_ai.service;

import com.spaced_repetition_ai.dto.UserResponseDTO;
import com.spaced_repetition_ai.entity.UserEntity;
import com.spaced_repetition_ai.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor

public class UserService {
    private final UserRepository userRepository;



    public UserEntity getAuthenticatedUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado: " + email));
    }


    public UserResponseDTO getCurrentUserDetails() {
        UserEntity user = getAuthenticatedUser();
        return UserResponseDTO.fromEntity(user);
    }
}