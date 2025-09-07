package com.spaced_repetition_ai.storage;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class ImageStorage {

    private final String folder = "A:\\DeJavan\\spaced-repetition-ai\\Storage/";

    public String saveImage(String imageName, byte[] imageData) {
        Path path = Paths.get(folder + imageName);

        try {

            Files.createDirectories(path.getParent());
            Files.write(path, imageData);
            System.out.println("Arquivo de imagem salvo em: " + path.toAbsolutePath());

            return "/storage/" + imageName;
        } catch (IOException e) {
            System.err.println("Erro ao salvar a imagem: " + e.getMessage());

            throw new RuntimeException("Falha ao salvar a imagem: " + imageName, e);

        }
    }
}
