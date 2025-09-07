package com.spaced_repetition_ai.controller;

import com.spaced_repetition_ai.model.FlashCard;
import com.spaced_repetition_ai.service.TextGenerationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/text")
public class TextGenerationController {

    private final TextGenerationService textGenerationService;

    @Autowired
    public TextGenerationController(TextGenerationService taskGenerationService){
        this.textGenerationService = taskGenerationService;
    }

    @PostMapping("/generate")
    public ResponseEntity<FlashCard> generateTask(@RequestBody String userInput) {
        FlashCard generatedTask = textGenerationService.generateTextFromJson(userInput);
        return ResponseEntity.ok(generatedTask);
    }

    @PostMapping("/createMultipleFlashcards")
    public List<FlashCard> generateMultipleFlashcard(@RequestParam("prompt") String prompt, @RequestParam("numberOfTasks") int numberOfFlashcards) {
        return textGenerationService.createMultipleFlashcards(prompt, numberOfFlashcards);
    }
}
