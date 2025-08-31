package com.spaced_repetition_ai.entity;


import com.spaced_repetition_ai.model.AuthProvider;
import com.spaced_repetition_ai.model.Role;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users")
public class UserEntity implements UserDetails, OidcUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String name;
    @Column
    private String password;
    @Column(unique = true, nullable = false)
    private String email;
    @Enumerated(EnumType.STRING)
    private Role role;
    @Column(nullable = false, columnDefinition = "integer default 0")
    private int balance;
    @Enumerated(EnumType.STRING)
    @Column(name = "auth_provider", nullable = false)
    @ColumnDefault("'LOCAL'")
    private AuthProvider authProvider;


    @OneToMany(
            mappedBy = "user",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<DeckEntity> deckEntity = new ArrayList<>();


    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getUsername() {
        return email;
    }



    @Transient
    private Map<String, Object> claims;

    @Transient
    private OidcUserInfo userInfo;

    @Transient
    private OidcIdToken idToken;


    // --- Métodos de OAuth2User e OidcUser ---

    @Transient
    private Map<String, Object> attributes;

    @Override
    public String getName() {
        return this.name;
    }

    // ✅ 6. IMPLEMENTAR OS MÉTODOS ADICIONAIS EXIGIDOS PELO OidcUser
    @Override
    public Map<String, Object> getClaims() {
        return this.claims;
    }

    @Override
    public OidcUserInfo getUserInfo() {
        return this.userInfo;
    }

    @Override
    public OidcIdToken getIdToken() {
        return this.idToken;
    }
}
