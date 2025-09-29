package com.spaced_repetition_ai.service;

import com.spaced_repetition_ai.entity.UserEntity;
import com.spaced_repetition_ai.model.AuthProvider;
import com.spaced_repetition_ai.model.Role;
import com.spaced_repetition_ai.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomOidcUserService extends OidcUserService {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        // Carrega o usuário OIDC padrão do Google
        OidcUser oidcUser = super.loadUser(userRequest);

        String provider = userRequest.getClientRegistration().getRegistrationId();
        log.info("🔵 OIDC Provider detectado: {}", provider);

        // Extrair informações do Google
        String email = oidcUser.getEmail();
        String name = oidcUser.getFullName();

        // Fallback caso getFullName() retorne null
        if (name == null || name.isBlank()) {
            name = oidcUser.getAttribute("name");
        }

        // Fallback adicional para given_name + family_name
        if (name == null || name.isBlank()) {
            String givenName = oidcUser.getAttribute("given_name");
            String familyName = oidcUser.getAttribute("family_name");
            if (givenName != null && familyName != null) {
                name = givenName + " " + familyName;
            } else if (givenName != null) {
                name = givenName;
            }
        }

        log.info("Email extraído: {}", email);
        log.info("Nome extraído: {}", name);

        // Validação
        if (email == null || email.isBlank()) {
            log.error("❌ Email não encontrado no OIDC user");
            throw new OAuth2AuthenticationException("Não foi possível obter o e-mail do Google");
        }

        // Busca ou cria o usuário no banco
        Optional<UserEntity> userOptional = userRepository.findByEmail(email);
        UserEntity userEntity;

        if (userOptional.isPresent()) {
            userEntity = userOptional.get();
            log.info("✅ Usuário existente encontrado: {}", email);
        } else {
            // Registra novo usuário
            userEntity = registerNewUser(AuthProvider.GOOGLE, name, email);
            log.info("✅ Novo usuário criado: {}", email);
        }

        // Configura os atributos OIDC na UserEntity
        userEntity.setAttributes(oidcUser.getAttributes());
        userEntity.setClaims(oidcUser.getClaims());
        userEntity.setUserInfo(oidcUser.getUserInfo());
        userEntity.setIdToken(oidcUser.getIdToken());

        log.info("🎉 loadUser concluído com sucesso para: {}", email);

        return userEntity;
    }

    private UserEntity registerNewUser(AuthProvider authProvider, String name, String email) {
        UserEntity newUser = UserEntity.builder()
                .email(email)
                .name(name)
                .role(Role.USER)
                .authProvider(authProvider)
                .balance(60)
                .build();

        log.info("💾 Salvando novo usuário no banco: {}", email);

        try {
            UserEntity saved = userRepository.save(newUser);
            log.info("✅ Usuário salvo com sucesso - ID: {}", saved.getId());
            return saved;
        } catch (Exception e) {
            log.error("❌ Erro ao salvar usuário: {}", e.getMessage(), e);
            throw new OAuth2AuthenticationException("Falha ao registrar novo utilizador: " + e.getMessage());
        }
    }
}