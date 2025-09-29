package com.spaced_repetition_ai.config;

import com.spaced_repetition_ai.entity.UserEntity;
import com.spaced_repetition_ai.repository.UserRepository;
import com.spaced_repetition_ai.service.AwsService;
import com.spaced_repetition_ai.service.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final AwsService awsService;
    @Value("${cookie-domain}")
    private String cookieDomain;

    @Value("${app.oauth2.redirect-uri}")
    private String redirectUri;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
        UserEntity userEntity;
        Object principal = authentication.getPrincipal();

        log.info("Salvando usuario________222333");

        if (principal instanceof UserEntity) {
            userEntity = (UserEntity) principal;
        } else if (principal instanceof OAuth2User) {

            OAuth2User oauth2User = (OAuth2User) principal;
            String email = oauth2User.getAttribute("email");

            if (email == null) {
                throw new IllegalArgumentException("Não foi possível extrair o e-mail do usuário OAuth2.");
            }
            // Usamos o método com retentativa para evitar a condição de corrida
            userEntity = findUserAfterRegistration(email);

        } else {
            throw new IllegalArgumentException("Tipo de principal de autenticação não suportado: " + principal.getClass().getName());
        }



        String token = jwtService.generateToken(userEntity);

        Map<String, String> signedCookies = awsService.getSignedCloudFrontCookies(userEntity.getId());

        ResponseCookie keyPairId = ResponseCookie.from("CloudFront-Key-Pair-Id", signedCookies.get("CloudFront-Key-Pair-Id"))
                .domain(cookieDomain)
                .path("/")
                .httpOnly(true)
                .secure(true) // ESSENCIAL para SameSite=None
                .sameSite("None")
                .build();

        ResponseCookie signature = ResponseCookie.from("CloudFront-Signature", signedCookies.get("CloudFront-Signature"))
                .domain(cookieDomain)
                .path("/")
                .httpOnly(true)
                .secure(true) // ESSENCIAL para SameSite=None
                .sameSite("None")
                .build();

        ResponseCookie policy = ResponseCookie.from("CloudFront-Policy", signedCookies.get("CloudFront-Policy"))
                .domain(cookieDomain)
                .path("/")
                .httpOnly(true)
                .secure(true) // ESSENCIAL para SameSite=None
                .sameSite("None")
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, keyPairId.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, signature.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, policy.toString());

        String targetUrl = UriComponentsBuilder.fromUriString(redirectUri)
                .queryParam("token", token)
                .build().toUriString();

        clearAuthenticationAttributes(request);
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

    /**
     * Tenta encontrar o usuário no banco de dados com algumas retentativas.
     * Isso resolve a race condition onde este handler é executado antes do commit
     * da transação que salva o novo usuário.
     */
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