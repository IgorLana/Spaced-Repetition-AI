package com.spaced_repetition_ai.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Async
    public void sendVerificationEmail(String to, String token) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Verifique a sua conta");
        String verificationUrl = "http:/localhost:9090/api/auth/verify?token=" + token;
        message.setText("Para verificar sua conta, clique no link abaixo:\n" + verificationUrl);

        try {
            mailSender.send(message);
            log.info("Email enviado com sucesso para " + to);
        }catch (Exception e) {
            log.error("Erro ao enviar email para " + to, e);
        }
    }

}
