package com.spaced_repetition_ai.controller;

import com.spaced_repetition_ai.dto.AuthRequestDTO;
import com.spaced_repetition_ai.dto.AuthResponse;
import com.spaced_repetition_ai.dto.RegisterRequest;
import com.spaced_repetition_ai.entity.UserEntity;
import com.spaced_repetition_ai.model.AuthProvider;
import com.spaced_repetition_ai.model.Role;
import com.spaced_repetition_ai.repository.UserRepository;
import com.spaced_repetition_ai.service.AwsService;
import com.spaced_repetition_ai.service.EmailService;
import com.spaced_repetition_ai.service.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final AwsService awsService;
    private final EmailService emailService;
    @Value("${cookie-domain}")
    private String cookieDomain;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request){
        if (userRepository.findByEmail(request.email()).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new AuthResponse("Email já registrado", null));
        }
        String verificationToken = UUID.randomUUID().toString();
        var user = UserEntity.builder()
                .name(request.name())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .role(Role.USER)
                .balance(60)
                .authProvider(AuthProvider.LOCAL)
                .isVerified(false)
                .verificationToken(verificationToken)
                .tokenExpirationDate(LocalDateTime.now().plusMinutes(60))
                .build();
        userRepository.save(user);

        emailService.sendVerificationEmail(user.getEmail(), verificationToken);

        return ResponseEntity.ok(new AuthResponse("Registo bem-sucedido. Por favor, verifique seu e-mail para ativar a conta.", null));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> authenticate(@RequestBody AuthRequestDTO request){
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.email(),
                        request.password()
                )
        );
        var user = userRepository.findByEmail(request.email())
                .orElseThrow();
        if (!user.isVerified()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new AuthResponse("Conta não verificada. Por favor, verifique seu e-mail.", null));
        }

        var jwtToken = jwtService.generateToken(user);
        Map<String, String> signedCookies = awsService.getSignedCloudFrontCookies(user.getId());

        ResponseCookie keyPairId = ResponseCookie.from("CloudFront-Key-Pair-Id", signedCookies.get("CloudFront-Key-Pair-Id"))
                .domain(cookieDomain) // ex: cdn.seusite.com
                .path("/")
                .httpOnly(true)
                .secure(true)
                .sameSite("None")
                .build();

        ResponseCookie signature = ResponseCookie.from("CloudFront-Signature", signedCookies.get("CloudFront-Signature"))
                .domain(cookieDomain)
                .path("/")
                .httpOnly(true)
                .secure(true)
                .sameSite("None")
                .build();

        ResponseCookie policy = ResponseCookie.from("CloudFront-Policy", signedCookies.get("CloudFront-Policy"))
                .domain(cookieDomain)
                .path("/")
                .httpOnly(true)
                .secure(true)
                .sameSite("None")
                .build();

        System.out.println("Cookies: " + keyPairId.toString() + " " + signature.toString() + " " + policy.toString());

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, keyPairId.toString())
                .header(HttpHeaders.SET_COOKIE, signature.toString())
                .header(HttpHeaders.SET_COOKIE, policy.toString())
                .body(new AuthResponse(jwtToken, null)); // cookies não precisam mais ir no JSON

    }

    @GetMapping("/verify")
    public ResponseEntity<String> verifyUser(@RequestParam("token") String token) {
        return userRepository.findByVerificationToken(token)
                .map(user -> {
                    if (user.getTokenExpirationDate().isBefore(LocalDateTime.now())) {
                        user.setVerificationToken(null); // Limpa o token expirado
                        user.setTokenExpirationDate(null);
                        userRepository.save(user);
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("O link de verificação expirou. Por favor, solicite um novo.");
                    }

                    user.setVerified(true);
                    user.setVerificationToken(null);
                    userRepository.save(user);
                    return ResponseEntity.ok("Conta verificada com sucesso! Você já pode fazer login.");
                })
                .orElse(ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Token de verificação inválido."));
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<AuthResponse> resendVerificationEmail(@RequestParam("email") String email) {
        return userRepository.findByEmail(email)
                .map(user -> {
                    if (user.isVerified()) {
                        return ResponseEntity.status(HttpStatus.OK).body(new AuthResponse("Sua conta já está verificada.", null));
                    }

                    String newToken = UUID.randomUUID().toString();
                    user.setVerificationToken(newToken);
                    user.setTokenExpirationDate(LocalDateTime.now().plusHours(24));
                    userRepository.save(user);
                    emailService.sendVerificationEmail(user.getEmail(), newToken);

                    return ResponseEntity.ok(new AuthResponse("Novo e-mail de verificação enviado.", null));
                })
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body(new AuthResponse("E-mail não encontrado.", null)));
    }

}
