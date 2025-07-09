package com.spaced_repetition_ai.service;

import com.google.common.collect.ImmutableList;
import com.google.genai.Client;
import com.google.genai.types.*;
import com.spaced_repetition_ai.storage.AudioStorage;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.*;

@Service
public class AudioGenerationService {

    private final String audioGenerationModel = "gemini-2.5-flash-preview-tts";

    private final Client genaiClient;
    private final AudioStorage storageGenAi;

    public AudioGenerationService(Client genaiClient, AudioStorage storageGenAi) {
        this.genaiClient = genaiClient;
        this.storageGenAi = storageGenAi;
    }


    public List<String> generateAudio(String prompt, @Nullable List<MultipartFile> audios) {

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
                                                                .voiceName(voiceName) // String como "pt-BR-FabricioNeural"
                                                                .build()
                                                )
                                                .build()
                                )
                                .build()
                )
                .build();

        GenerateContentResponse response = this.genaiClient.models.generateContent(this.audioGenerationModel, content, config);
        List<Audio> generatedAudio = getAudio(response);
        List<String> savedAudioPath = new ArrayList<>();
        for (Audio audio : generatedAudio) {
            String audioName = audio.audioName().replace("\\.[a-zA-Z0-9]+$", "") + ".wav";
            String fullAudioPath = this.storageGenAi.StorageWav(audioName, audio.audioData());
            savedAudioPath.add(fullAudioPath);
            System.out.println("Audio salvo: %s".formatted(fullAudioPath));
        }
        return savedAudioPath;
    }

    private List<Audio> getAudio(GenerateContentResponse response) {
        ImmutableList<Part> responseParts = response.parts();
        if (responseParts == null || responseParts.isEmpty()) {
            return Collections.emptyList();
        }
        return responseParts
                .stream()
                .map(Part::inlineData)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .filter(inlineData -> inlineData.data().isPresent())
                .map(inlineData -> {
                    MimeType mimeType = MimeType.valueOf(inlineData.mimeType().get()); // imageMimeType
                    return new Audio(
                            "%s.%s".formatted(UUID.randomUUID().toString(), mimeType.getSubtype()),
                            inlineData.data().get(), // imageBytes
                            mimeType.toString());
                })
                .toList();
    }

    record Audio(String audioName, byte[] audioData, String audioMimeType){}


}
