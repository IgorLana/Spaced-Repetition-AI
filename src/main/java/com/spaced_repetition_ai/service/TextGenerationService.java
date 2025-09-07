package com.spaced_repetition_ai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;
import com.spaced_repetition_ai.model.FlashCard;
import com.spaced_repetition_ai.model.TextPromptStyle;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.core.type.TypeReference;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
public class TextGenerationService {

    private final Client genaiClient;


    public TextGenerationService(Client genaiClient) {
        this.genaiClient = genaiClient;
    }

    public FlashCard generateTextFromJson(String prompt) {

        System.out.println(prompt);

        GenerateContentResponse response = GenerateTextFromTextInput(prompt);
        String generatedJsonString = response.text();
        System.out.println("JSON recebido do Gemini (bruto): " + generatedJsonString);

        assert generatedJsonString != null;
        if (generatedJsonString.startsWith("```json")) {
            generatedJsonString = generatedJsonString.substring("```json".length()).trim();
        }
        if (generatedJsonString.endsWith("```")) {
            generatedJsonString = generatedJsonString.substring(0, generatedJsonString.length() - "```".length()).trim();
        }
        System.out.println("JSON após remoção dos delimitadores: " + generatedJsonString);
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        try {
            FlashCard generatedText = objectMapper.readValue(generatedJsonString, FlashCard.class);
            return generatedText;
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Async
    public CompletableFuture<FlashCard> generateTextFromJsonAsync(String prompt) {
        try {
            FlashCard result = generateTextFromJson(prompt);
            return CompletableFuture.completedFuture(result);
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }



    public GenerateContentResponse GenerateTextFromTextInput(String prompt) {
        GenerateContentResponse response =
                genaiClient.models.generateContent(
                        "gemini-2.5-flash",
                        prompt,
                        null);
        System.out.println(response.text());
        return response;
    }

    public List<FlashCard> createMultipleFlashcards(String prompt, long nFlashcard) {

        assert prompt != null;
        try {
            String promptTemplate = TextPromptStyle.MultipleFlashcards.getTemplate();
            String promptFinal = promptTemplate
                    .replace("{numero_de_flashcards}", String.valueOf(nFlashcard))
                    .replace("{prompt_do_usuario}", prompt);
            System.out.println(promptFinal);
            String generatedJsonString = GenerateTextFromTextInput(promptFinal).text();

            System.out.println("JSON recebido do Gemini (bruto): " + generatedJsonString);

            assert generatedJsonString != null;
            if (generatedJsonString.startsWith("```json")) {
                generatedJsonString = generatedJsonString.substring("```json".length()).trim();
            }
            if (generatedJsonString.endsWith("```")) {
                generatedJsonString = generatedJsonString.substring(0, generatedJsonString.length() - "```".length()).trim();
            }
            System.out.println("JSON após remoção dos delimitadores: " + generatedJsonString);

            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.registerModule(new JavaTimeModule());

            try {
                List<FlashCard> generatedList = objectMapper.readValue(generatedJsonString, new TypeReference<List<FlashCard>>() {
                });
                return generatedList;
            } catch (JsonProcessingException e) {
                e.printStackTrace();
                return null;
            }
        } catch (Exception e) {
            log.error("Erro ao tentar obter o template do deck.", e);
            System.out.print("Ocorreu um erro ao tentar obter o template do deck. Tente novamente.");
            return null;
        }
    }
}
