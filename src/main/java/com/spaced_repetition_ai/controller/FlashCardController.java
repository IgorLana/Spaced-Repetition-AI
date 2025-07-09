package com.spaced_repetition_ai.controller;

import com.spaced_repetition_ai.service.FlashCardService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/flashcard")
public class FlashCardController {

    private final FlashCardService flashCardService;

    public FlashCardController(FlashCardService flashCardService) {
        this.flashCardService = flashCardService;
    }

    @PostMapping
    public void generateFlashCard(@RequestParam("front") String front, @RequestParam("back")String back) {
        flashCardService.generateFlashCard(front, back, null, null);

    }

    @PostMapping("/ai")
    public ResponseEntity<FlashCardService.GeneratedFlashCard> generateAiFlashCard(@RequestParam("prompt") String prompt) {
        try {
            FlashCardService.GeneratedFlashCard generatedCard = flashCardService.generateAiFlashCard(prompt);
            return ResponseEntity.ok(generatedCard); // Retorna 200 OK com os dados do flashcard em JSON
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

    }

}
