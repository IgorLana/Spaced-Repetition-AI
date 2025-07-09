package com.spaced_repetition_ai.controller;


import com.spaced_repetition_ai.model.FlashCard;
import com.spaced_repetition_ai.service.TextGenerationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/text")
public class TextGenerationController {

    private final TextGenerationService taskGenerationService;

    @Autowired
    public TextGenerationController(TextGenerationService taskGenerationService){
        this.taskGenerationService = taskGenerationService;
    }

    @PostMapping("/generate")
    public ResponseEntity<FlashCard> generateTask(@RequestBody String userInput) {
        // 1º Usuário descreve a necessidade dele (o userInput já é recebido aqui)
        // 2º e 3º A lógica de chamar a API Gemini será feita no serviço
        FlashCard generatedTask = taskGenerationService.generateTextFromJson(userInput);
        // Enviamos esse JSON para o front-end do usuário.
        return ResponseEntity.ok(generatedTask);
    }
}
