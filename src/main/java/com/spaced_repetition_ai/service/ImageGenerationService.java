package com.spaced_repetition_ai.service;

import com.google.common.collect.ImmutableList;
import com.google.genai.Client;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Part;
import com.spaced_repetition_ai.storage.ImageStorage;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

@Service
public class ImageGenerationService {


    private final String imageGenerationModel = "gemini-2.0-flash-preview-image-generation";

    private final Client genaiClient;
    private final ImageStorage imageStorage;

    public ImageGenerationService(Client genaiClient, ImageStorage imageStorage) {
        this.genaiClient = genaiClient;
        this.imageStorage = imageStorage;
    }



    public List<String> generateImage(String prompt, @Nullable List<MultipartFile> images){

        System.out.println("Imagem a ser gerada: %s".formatted(prompt));

        List<Part> parts = new ArrayList<>();
        parts.add(Part.fromText(prompt));

        if (images != null) {
            List<Part> imagePart = images.stream()
                    .map( image -> {
                        try {
                            return Part.fromBytes(image.getBytes(), image.getContentType());
                        } catch (IOException e) {
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .toList();
            parts.addAll(imagePart);
        }

        Content content = Content.builder().parts(parts).build();
        GenerateContentConfig config = GenerateContentConfig.builder()
                .responseModalities(List.of("Text", "Image"))
                .build();

        GenerateContentResponse response = this.genaiClient.models.generateContent(imageGenerationModel, content, config);
        List<Image> generateImage = getImages(response);

        List<String> savedImagePath = new ArrayList<>();
        for (Image image : generateImage) {
            String fullPath = imageStorage.saveImage(image.imageName(), image.imageBytes());
            savedImagePath.add(fullPath);
            System.out.println("Imagem salva: %s".formatted(fullPath));
        }
        return savedImagePath;
    }

    private List<Image> getImages(GenerateContentResponse response) {
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
                    return new Image(
                            "%s.%s".formatted(UUID.randomUUID().toString(), mimeType.getSubtype()),
                            inlineData.data().get(), // imageBytes
                            mimeType.toString());
                })
                .toList();
    }

    record Image(String imageName, byte[] imageBytes, String mimeType) {}

}
