package com.spaced_repetition_ai.service;

import com.spaced_repetition_ai.entity.UserEntity;
import com.spaced_repetition_ai.model.AuthProvider;
import com.spaced_repetition_ai.model.Role;
import com.spaced_repetition_ai.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomOAuth2UserService extends DefaultOAuth2UserService {
    private final UserRepository userRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = super.loadUser(userRequest);

        // Identifica qual provedor está sendo usado (ex: "google", "github")
        String provider = userRequest.getClientRegistration().getRegistrationId();

        String email;
        String name;
        AuthProvider authProvider;

        System.out.println("Testando 1 ___________" + oauth2User + "Test 2___________" + userRequest);
        System.out.println("Testando 2 ___________" + oauth2User.getAttributes());
        System.out.println("Testando 3 ___________" + oauth2User.getAttribute("email"));
        System.out.println("Testando 4 ___________" + oauth2User.getAttribute("name"));
        System.out.println("Testando 5 ___________" + userRequest.getClientRegistration().getRegistrationId());

        if ("github".equalsIgnoreCase(provider)) {
            // Lógica para extrair dados do GitHub
            authProvider = AuthProvider.GITHUB;
            // O e-mail do GitHub pode ser nulo se não for público
            email = oauth2User.getAttribute("email");
            name = oauth2User.getAttribute("name");
            if (name == null || name.isBlank()) {
                name = oauth2User.getAttribute("login"); // Usa o login como fallback
            }
            // ✅ LÓGICA ATUALIZADA PARA BUSCAR E-MAIL
            email = oauth2User.getAttribute("email");
            if (email == null || email.isBlank()) {
                log.info("E-mail não encontrado no perfil principal do GitHub. Tentando endpoint /user/emails...");
                email = getPrimaryEmailFromGitHub(userRequest.getAccessToken());
            }

        } else if ("google".equalsIgnoreCase(provider)) {
            // Lógica para extrair dados do Google (mantém como estava)
            authProvider = AuthProvider.GOOGLE;
            email = oauth2User.getAttribute("email");
            name = oauth2User.getAttribute("name");

        } else {
            throw new OAuth2AuthenticationException("Provedor de login não suportado: " + provider);
        }

        if (email == null || email.isBlank()) {
            throw new OAuth2AuthenticationException("Não foi possível obter o e-mail do provedor " + provider);
        }

        Optional<UserEntity> userOptional = userRepository.findByEmail(email);
        UserEntity userEntity;

        if (userOptional.isPresent()) {
            userEntity = userOptional.get();
        } else {
            // Se o usuário não existe, registra um novo
            userEntity = registerNewUser(authProvider, name, email);
        }

        // Atribui os atributos do usuário OAuth2 à nossa entidade para uso posterior
        userEntity.setAttributes(oauth2User.getAttributes());

        return userEntity;
    }

    private UserEntity registerNewUser(AuthProvider authProvider, String name, String email) {
        UserEntity newUser = UserEntity.builder()
                .email(email)
                .name(name)
                .role(Role.USER)
                .authProvider(authProvider)
                .balance(300)
                .build();
        try {
            return userRepository.save(newUser);
        } catch (Exception e) {
            throw new OAuth2AuthenticationException("Falha ao registar novo utilizador. Detalhes: " + e.getMessage());
        }
    }

    private String getPrimaryEmailFromGitHub(OAuth2AccessToken accessToken) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            // Adiciona o token de acesso no cabeçalho da requisição
            headers.setBearerAuth(accessToken.getTokenValue());
            HttpEntity<String> entity = new HttpEntity<>("", headers);

            // Faz a chamada para o endpoint /user/emails
            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                    "https://api.github.com/user/emails",
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {}
            );

            // Procura na resposta o e-mail que é primário e verificado
            if (response.getBody() != null) {
                return response.getBody().stream()
                        .filter(emailMap -> (Boolean) emailMap.getOrDefault("primary", false) && (Boolean) emailMap.getOrDefault("verified", false))
                        .map(emailMap -> (String) emailMap.get("email"))
                        .findFirst()
                        .orElse(null);
            }
        } catch (Exception e) {
            log.error("Erro ao buscar e-mails da API do GitHub: {}", e.getMessage());
        }
        return null;
    }
}