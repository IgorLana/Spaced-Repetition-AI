package com.spaced_repetition_ai.controller;

import com.spaced_repetition_ai.dto.UserResponseDTO;
import com.spaced_repetition_ai.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<UserResponseDTO> getCurrentUser() {
        UserResponseDTO userDetails = userService.getCurrentUserDetails();
        return ResponseEntity.ok(userDetails);
    }
}