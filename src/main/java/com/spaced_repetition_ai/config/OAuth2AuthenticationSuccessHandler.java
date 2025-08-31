package com.spaced_repetition_ai.config;

import com.spaced_repetition_ai.entity.UserEntity;
import com.spaced_repetition_ai.repository.UserRepository;
import com.spaced_repetition_ai.service.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
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
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtService jwtService;
    private final UserRepository userRepository; // Adicionado para buscar o usuário

    @Value("${app.oauth2.redirect-uri}")
    private String redirectUri;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
        UserEntity userEntity;
        Object principal = authentication.getPrincipal();
        System.out.println("Testando 1 ___________" + request + "Test 2___________" + response);

        // ✅ LÓGICA ATUALIZADA PARA LIDAR COM MÚLTIPLOS PROVEDORES
        if (principal instanceof UserEntity) {
            // Se o principal já é nossa entidade (caso do GitHub), usamos diretamente.
            userEntity = (UserEntity) principal;
        } else if (principal instanceof OAuth2User) {
            // Se for um OidcUser (caso do Google) ou outro OAuth2User, extraímos o e-mail
            // e buscamos nossa entidade no banco de dados para garantir que temos o objeto correto.
            OAuth2User oauth2User = (OAuth2User) principal;
            String email = oauth2User.getAttribute("email");
            System.out.println("teste 3 ___________" + email);
            if (email == null) {
                throw new IllegalArgumentException("Não foi possível extrair o e-mail do usuário OAuth2.");
            }
            // Usamos o método com retentativa para evitar a condição de corrida
            userEntity = findUserAfterRegistration(email);
            System.out.println("teste 3 ___________" + email);
        } else {
            throw new IllegalArgumentException("Tipo de principal de autenticação não suportado: " + principal.getClass().getName());
        }




        // A partir daqui, o fluxo é o mesmo
        String token = jwtService.generateToken(userEntity);

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
        int retries = 5;
        long delay = 100;

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