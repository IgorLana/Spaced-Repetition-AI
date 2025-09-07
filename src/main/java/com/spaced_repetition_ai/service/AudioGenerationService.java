package com.spaced_repetition_ai.service;


import com.google.genai.Client;
import com.google.genai.types.*;
import com.spaced_repetition_ai.entity.UserEntity;
import com.spaced_repetition_ai.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
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

    public AudioGenerationService(Client genaiClient, UserRepository userRepository) {
        this.genaiClient = genaiClient;
        this.userRepository = userRepository;
    }

    public record GeneratedAudioData(byte[] audioBytes, String mimeType) {}

    public GeneratedAudioData generateAudio(String prompt, @Nullable List<MultipartFile> audios, Long userId) {
        UserEntity usuarioLogado = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado: " + userId));

        if (usuarioLogado.getBalance() < 1){
            log.info("Saldo insuficiente para gerar audio.");
            return null;
        }

        if (prompt.isBlank()){
            log.info("Nao é possivel gerar audio com prompt vazio.");
            return null;
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

        try{
            GenerateContentResponse response = this.genaiClient.models.generateContent(this.audioGenerationModel, content, config);
            byte[] rawAudioBytes = response.parts().get(0).inlineData().get().data().get();
            byte[] wavBytes = convertToWav(rawAudioBytes);
            usuarioLogado.setBalance(usuarioLogado.getBalance() - 1);
            log.info("Audio gerado com sucesso!");
            return new GeneratedAudioData(wavBytes, "audio/wav");
        }catch (Exception e){
            log.error("API do Google fora do ar!", e);
            log.info("Ocorreu um erro ao gerar o audio. Tente novamente.");
            return null;
        }
    }

    @Async
    public CompletableFuture<GeneratedAudioData> generateAudioAsync(String prompt, @Nullable List<MultipartFile> audios, Long userId) {
        try {
            GeneratedAudioData result = generateAudio(prompt, audios, userId);
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
