package com.spaced_repetition_ai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;
import com.spaced_repetition_ai.model.FlashCard;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
public class TextGenerationService {

    private final Client genaiClient;


    public TextGenerationService(Client genaiClient, AudioGenerationService audioGenAiService, ImageGenerationService imageGenAiService) {
        this.genaiClient = genaiClient;
    }

    public FlashCard generateTextFromJson(String prompt) {

        System.out.println(prompt);


        GenerateContentResponse response = GenerateTextFromTextInput(prompt);
        String generatedJsonString = response.text();
        System.out.println("JSON recebido do Gemini (bruto): " + generatedJsonString);

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

            String flashCard = "Front: " + generatedText.getFront() + "\nBack: " + generatedText.getBack();


            return generatedText;
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return null;
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
}
