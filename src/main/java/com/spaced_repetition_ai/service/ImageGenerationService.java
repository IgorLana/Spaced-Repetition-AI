package com.spaced_repetition_ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import com.google.genai.Client;
import com.google.genai.types.*;
import com.spaced_repetition_ai.entity.UserEntity;

import com.spaced_repetition_ai.exception.ExternalServiceException;
import com.spaced_repetition_ai.model.ImageStyle;

import com.spaced_repetition_ai.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.lang.Nullable;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;

import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
public class ImageGenerationService {


    private final Client genaiClient;
    private final UserRepository userRepository;
    private final ApplicationContext applicationContext;

    public record GeneratedImageData(byte[] imageBytes, String mimeType) {}

    public ImageGenerationService(Client genaiClient, UserRepository userRepository, ApplicationContext applicationContext) {
        this.genaiClient = genaiClient;
        this.userRepository = userRepository;
        this.applicationContext = applicationContext;
    }


    @Retryable(
            retryFor = ExternalServiceException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000, multiplier = 2)
    )
    public GeneratedImageData generateImage(String prompt, @Nullable List<MultipartFile> images, ImageStyle style, Long userId) {
        try {
            UserEntity usuarioLogado = userRepository.findById(userId)
                    .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado: " + userId));
            if (usuarioLogado.getBalance() < 5) {
                throw new IllegalStateException("Saldo insuficiente para gerar imagem.");
            }
            if (prompt.isBlank()) {
                throw new IllegalArgumentException("Não é possível gerar imagem com prompt vazio.");
            }


            String promptFinal = prompt;
            try {
                String template = style.getTemplate();
                promptFinal = template
                        .replace("${language}", "ingles")
                        .replace("${word}", prompt);

                GenerateContentResponse response =
                        genaiClient.models.generateContent(
                                "gemini-2.5-flash",
                                promptFinal,
                                null);

                System.out.println(response.text());
                String generatedJsonString = response.text();
                System.out.println("JSON recebido do Gemini (bruto): " + generatedJsonString);
                assert generatedJsonString != null;
                if (generatedJsonString.startsWith("```json")) {
                    generatedJsonString = generatedJsonString.substring("```json" .length()).trim();
                }
                if (generatedJsonString.endsWith("```")) {
                    generatedJsonString = generatedJsonString.substring(0, generatedJsonString.length() - "```" .length()).trim();
                }
                System.out.println("JSON após remoção dos delimitadores: " + generatedJsonString);

                promptFinal = generatedJsonString;
                ObjectMapper objectMapper = new ObjectMapper();
                objectMapper.registerModule(new JavaTimeModule());

            } catch (Exception e) {
                log.error("Erro ao tentar obter o template do deck.", e);
                System.out.print("Ocorreu um erro ao tentar obter o template do deck. Tente novamente.");
            }
            System.out.println("Prompt final:____________________________________________________" + promptFinal);

            GenerateImagesConfig config = GenerateImagesConfig
                    .builder()
                    .numberOfImages(1)
                    .aspectRatio("1:1")
                    .outputMimeType("image/png")
                    .build();
            GenerateImagesResponse response = this.genaiClient.models.generateImages("imagen-4.0-fast-generate-001", promptFinal, config);

            if (response.generatedImages().isEmpty() ||
                    response.generatedImages().get().isEmpty()) {
                log.error("API retornou lista vazia de imagens");
                throw new ExternalServiceException("Falha ao gerar imagem: resposta vazia da API");
            }

            byte[] imageBytesList = response.generatedImages().get().get(0).image().get().imageBytes().get();

            usuarioLogado.setBalance(usuarioLogado.getBalance() - 5);
            userRepository.save(usuarioLogado);
            log.info("Imagem gerada com sucesso!");

            return new GeneratedImageData(imageBytesList, "image/png");

        } catch (ExternalServiceException e) {
            log.warn("Tentativa falhou, será retentada: {}", e.getMessage());
            throw e;
        } catch (IllegalStateException | IllegalArgumentException e) {
            log.error("Erro de validação: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Erro inesperado ao gerar imagem", e);
            throw new ExternalServiceException("Erro ao gerar imagem: " + e.getMessage(), e);
        }
    }

    @Async
    public CompletableFuture<GeneratedImageData> generateImageAsync(String prompt, @Nullable List<MultipartFile> images, ImageStyle style, Long userId) {
        try {
            ImageGenerationService self = applicationContext.getBean(ImageGenerationService.class);
            GeneratedImageData result = self.generateImage(prompt, images, style, userId);
            return CompletableFuture.completedFuture(result);
        } catch (Exception e) {
            log.error("Falha na geração assíncrona de imagem", e);
            return CompletableFuture.failedFuture(e);
        }
    }

}


