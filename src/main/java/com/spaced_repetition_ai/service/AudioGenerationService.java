package com.spaced_repetition_ai.service;


import com.google.genai.Client;
import com.google.genai.types.*;
import com.spaced_repetition_ai.entity.UserEntity;
import com.spaced_repetition_ai.exception.ExternalServiceException;
import com.spaced_repetition_ai.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;

import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Nullable;
import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
public class AudioGenerationService {

    private final String audioGenerationModel = "gemini-2.5-flash-preview-tts";

    private final Client genaiClient;
    private final UserRepository userRepository;
    private final ApplicationContext applicationContext;

    public AudioGenerationService(Client genaiClient, UserRepository userRepository
    , ApplicationContext applicationContext) {
        this.genaiClient = genaiClient;
        this.userRepository = userRepository;
        this.applicationContext = applicationContext;
    }

    public record GeneratedAudioData(byte[] audioBytes, String mimeType) {}



    @Retryable(
            retryFor = ExternalServiceException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000, multiplier = 2)
    )
    public GeneratedAudioData generateAudio(String prompt, @Nullable List<MultipartFile> audios, Long userId) {
        try{
        UserEntity usuarioLogado = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado: " + userId));

            if (usuarioLogado.getBalance() < 1) {
                throw new IllegalStateException("Saldo insuficiente para gerar áudio.");
            }

            // CORRIGIDO: lançar exceção em vez de retornar null
            if (prompt.isBlank()) {
                throw new IllegalArgumentException("Não é possível gerar áudio com prompt vazio.");
            }

        List<Part> parts = new ArrayList<>();
        parts.add(Part.fromText(prompt));

        if (audios != null)  {
            List<Part> audioParts = audios.stream()
                    .map( audio -> {
                        try {
                            return Part.fromBytes(audio.getBytes(), audio.getContentType());
                        } catch(IOException e) {
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .toList();
            parts.addAll(audioParts);
        }

        Content content = Content.builder().parts(parts).build();
        String voiceName = "kore";
        GenerateContentConfig config = GenerateContentConfig.builder()
                .responseModalities(List.of("Audio"))
                .speechConfig(
                        SpeechConfig.builder()
                                .voiceConfig(
                                        VoiceConfig.builder()
                                                .prebuiltVoiceConfig(
                                                        PrebuiltVoiceConfig.builder()
                                                                .voiceName(voiceName)
                                                                .build()
                                                )
                                                .build()
                                )
                                .build()
                )
                .build();


            GenerateContentResponse response = this.genaiClient.models.generateContent(this.audioGenerationModel, content, config);

            // VALIDAÇÃO CRÍTICA
            if (response.parts().isEmpty() ||
                    response.parts().get(0).inlineData().isEmpty() ||
                    response.parts().get(0).inlineData().get().data().isEmpty()) {
                log.error("API retornou resposta vazia de áudio");
                throw new ExternalServiceException("Falha ao gerar áudio: resposta vazia da API");
            }

            byte[] rawAudioBytes = response.parts().get(0).inlineData().get().data().get();
            byte[] wavBytes = convertToWav(rawAudioBytes);

            usuarioLogado.setBalance(usuarioLogado.getBalance() - 1);
            userRepository.save(usuarioLogado);
            log.info("Áudio gerado com sucesso!");

            return new GeneratedAudioData(wavBytes, "audio/wav");

        } catch (ExternalServiceException e) {
            log.warn("Tentativa de geração de áudio falhou, será retentada: {}", e.getMessage());
            throw e;
        } catch (IllegalStateException | IllegalArgumentException e) {
            log.error("Erro de validação: {}", e.getMessage());
            throw e;
        } catch (IOException | UnsupportedAudioFileException e) {
            log.error("Erro ao converter áudio para WAV", e);
            throw new ExternalServiceException("Erro ao processar áudio: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Erro inesperado ao gerar áudio", e);
            throw new ExternalServiceException("Erro ao gerar áudio: " + e.getMessage(), e);
        }
    }

    @Async
    public CompletableFuture<GeneratedAudioData> generateAudioAsync(String prompt, @Nullable List<MultipartFile> audios, Long userId) {
        try {
            AudioGenerationService self = applicationContext.getBean(AudioGenerationService.class);
            GeneratedAudioData result = self.generateAudio(prompt, audios, userId);
            return CompletableFuture.completedFuture(result);
        } catch (Exception e) {
            log.error("Falha na geração assíncrona de áudio", e);
            return CompletableFuture.failedFuture(e);
        }
    }

    private byte[] convertToWav(byte[] rawAudioData) throws IOException, UnsupportedAudioFileException {
        AudioFormat format = new AudioFormat(
                24000f,   // A frequência padrão para o modelo TTS do Gemini é 24kHz
                16,       // 16 bits por amostra
                1,        // 1 para mono
                true,     // signed
                false     // little-endian
        );

        try (
                ByteArrayInputStream bais = new ByteArrayInputStream(rawAudioData);
                AudioInputStream ais = new AudioInputStream(bais, format, rawAudioData.length / format.getFrameSize());
                ByteArrayOutputStream baos = new ByteArrayOutputStream()
        ) {
            AudioSystem.write(ais, AudioFileFormat.Type.WAVE, baos);
            return baos.toByteArray();
        }
    }

}
