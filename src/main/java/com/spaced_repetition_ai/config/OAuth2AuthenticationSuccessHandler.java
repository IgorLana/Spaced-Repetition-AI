package com.spaced_repetition_ai.config;

import com.spaced_repetition_ai.entity.UserEntity;
import com.spaced_repetition_ai.repository.UserRepository;
import com.spaced_repetition_ai.service.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Value("${app.oauth2.redirect-uri}")
    private String redirectUri;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
        UserEntity userEntity;
        Object principal = authentication.getPrincipal();


        if (principal instanceof UserEntity) {
            userEntity = (UserEntity) principal;
        } else {
            throw new IllegalArgumentException("Tipo de principal de autenticação não suportado: " + principal.getClass().getName());
        }

        String token = jwtService.generateToken(userEntity);

        String targetUrl = UriComponentsBuilder.fromUriString(redirectUri)
                .queryParam("token", token)
                .build().toUriString();

        clearAuthenticationAttributes(request);
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }



    private UserEntity findUserAfterRegistration(String email) {
        int retries = 20;
        long delay = 500;

        log.info("Salvando usuario________22");


        for (int i = 0; i < retries; i++) {
            Optional<UserEntity> userOptional = userRepository.findByEmail(email);
            if (userOptional.isPresent()) {
                return userOptional.get();
            }
            try {
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
        }
        throw new IllegalArgumentException("Usuário OAuth2 não encontrado no banco de dados após " + retries + " tentativas.");
    }
}