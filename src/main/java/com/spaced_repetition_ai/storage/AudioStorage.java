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


    private final String folder = "Storage/";

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
            return path.toAbsolutePath().toString();
        } catch (IOException e) {
            System.out.println("Erro ao salvar WAV: " + e.getMessage());
            return null;
        }

    }


}
