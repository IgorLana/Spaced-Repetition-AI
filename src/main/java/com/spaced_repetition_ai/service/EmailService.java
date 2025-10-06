package com.spaced_repetition_ai.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;

import org.thymeleaf.context.Context;


@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {


    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Async
    public void sendVerificationEmail(String to, String name, String token) {
        log.info("A preparar email de verificação para {}", to);

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "UTF-8");

            String verificationUrl = "https://www.mnemus.tech/api/auth/verify?token=" + token;

            // Criar o contexto do Thymeleaf com as variáveis
            Context context = new Context();
            context.setVariable("name", name);
            context.setVariable("verificationUrl", verificationUrl);

            // Processar o template HTML com o contexto
            String htmlContent = templateEngine.process("email/register.html", context);

            helper.setTo(to);
            helper.setSubject("Bem-vindo ao Spaced Repetition AI! Por favor, verifique a sua conta.");
            helper.setText(htmlContent, true); // O 'true' indica que o conteúdo é HTML
            helper.setFrom("contato@mnemus.tech");

            mailSender.send(mimeMessage);
            log.info("Email de verificação enviado com sucesso para {}", to);

        } catch (MessagingException e) {
            log.error("Erro ao enviar email de verificação para {}: {}", to, e.getMessage());
            // Lançar uma exceção para que o retentativa (retry) possa funcionar, se configurado
            throw new RuntimeException("Falha ao enviar e-mail.", e);
        }
    }
}
