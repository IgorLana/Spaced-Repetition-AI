package com.spaced_repetition_ai.storage;

import org.springframework.stereotype.Service;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class AudioStorage {


    private final String folder = "A:\\DeJavan\\spaced-repetition-ai\\Storage/";

    public String saveAudioFile(String audioName, byte[] audioData) {
        Path path = Paths.get(folder, audioName);
        try {
            Files.createDirectories(path.getParent());
            Files.write(path, audioData);
            System.out.println("Arquivo de áudio salvo em: " + path.toAbsolutePath());
            return "/storage/" + audioName;
        } catch (IOException e) {
            System.err.println("Erro ao salvar o arquivo de áudio: " + e.getMessage());
            throw new RuntimeException("Falha ao salvar o arquivo de áudio: " + audioName, e);
        }
    }

    public String StorageWav(String audioName, byte[] audioData){
        AudioFormat format = new AudioFormat(
                22500f,       // Ex: 22500f
                16,               // 16 bits por amostra
                1,         // 1 para mono, 2 para estéreo
                true,             // signed
                false             // little endian (padrão WAV)
        );

        try (
                ByteArrayInputStream bais = new ByteArrayInputStream(audioData);
                AudioInputStream audioInputStream = new AudioInputStream(bais, format, audioData.length / format.getFrameSize())
        ) {
            Path path = Paths.get(folder, audioName);
            Files.createDirectories(path.getParent()); // garante que a pasta existe
            AudioSystem.write(audioInputStream, AudioFileFormat.Type.WAVE, path.toFile());
            System.out.println("Áudio salvo como WAV em: " + path.toAbsolutePath());
            return "/storage/" + audioName;
        } catch (IOException e) {
            System.out.println("Erro ao salvar WAV: " + e.getMessage());
            return null;
        }

    }


}
