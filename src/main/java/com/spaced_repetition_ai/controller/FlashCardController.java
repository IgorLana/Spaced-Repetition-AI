package com.spaced_repetition_ai.controller;

import com.spaced_repetition_ai.dto.FlashcardRequestDTO;
import com.spaced_repetition_ai.entity.FlashCardEntity;
import com.spaced_repetition_ai.service.FlashCardService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/flashcard")
public class FlashCardController {

    private final FlashCardService flashCardService;

    public FlashCardController(FlashCardService flashCardService) {

        this.flashCardService = flashCardService;

    }

    @DeleteMapping("/{flashCardId}")
    public void deleteDeck(@PathVariable("flashCardId") String flashCardId) {
        flashCardService.deleteFlashCard(flashCardId);
    }

    @GetMapping
    public ResponseEntity<List<FlashCardEntity>> listFlashCardByDeck(@RequestParam("deckId") String deckId ){
        List<FlashCardEntity> toReview = flashCardService.listFlashCardsByDeck(deckId);

        if(toReview.isEmpty()){
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(toReview);
    }

    @PutMapping("/{id}") // Ou @PatchMapping para atualizações parciais
    public ResponseEntity<FlashCardEntity> update(@PathVariable("id") String id, @RequestBody FlashcardRequestDTO dto) {
        flashCardService.updateFlashCard(id, dto.front(), dto.back(), dto.imagePath(), dto.audioPath());
        return ResponseEntity.ok().build();
    }


    @PostMapping
    public void generateFlashCard(@RequestParam("front") String front, @RequestParam("back")String back) {
        flashCardService.generateFlashCard(front, back, null, null);

    }

    @PostMapping("/ai")
    public ResponseEntity<FlashCardService.GeneratedFlashCard> generateAiFlashCard(@RequestParam("prompt") String prompt, @RequestParam("deckId") String deckId) {
        try {
            FlashCardService.GeneratedFlashCard generatedCard = flashCardService.generateAiFlashCard(prompt, deckId);
            return ResponseEntity.ok(generatedCard); // Retorna 200 OK com os dados do flashcard em JSON
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

    }

}
