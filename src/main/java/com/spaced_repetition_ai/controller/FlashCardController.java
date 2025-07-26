package com.spaced_repetition_ai.controller;

import com.spaced_repetition_ai.dto.FlashcardRequestDTO;
import com.spaced_repetition_ai.dto.FlashcardResponseDTO;
import com.spaced_repetition_ai.entity.FlashCardEntity;
import com.spaced_repetition_ai.service.FlashCardService;
import jakarta.validation.Valid;
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
    public ResponseEntity<List<FlashcardResponseDTO>> listFlashCardByDeck(@RequestParam("deckId") String deckId ){
        List<FlashcardResponseDTO> toReview = flashCardService.listFlashCardsByDeck(deckId);

        if(toReview.isEmpty()){
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(toReview);
    }

    @PutMapping("/{id}") // Ou @PatchMapping para atualizações parciais
    public ResponseEntity<FlashCardEntity> update(@PathVariable("id") String id, @RequestBody FlashcardRequestDTO dto) {
        flashCardService.updateFlashCard(id, dto);
        return ResponseEntity.ok().build();
    }


    @PostMapping
    public void generateFlashCard(@RequestParam("deckId") String deckId, @RequestBody @Valid FlashcardRequestDTO dto) {
        flashCardService.generateFlashCard(deckId, dto);

    }

    @PostMapping("/ai")
    public ResponseEntity<FlashcardResponseDTO> generateAiFlashCard(@RequestParam("prompt") String prompt, @RequestParam("deckId") String deckId) {
        try {
            FlashcardResponseDTO generatedCard = flashCardService.generateAiFlashCard(prompt, deckId);
            return ResponseEntity.ok(generatedCard); // Retorna 200 OK com os dados do flashcard em JSON
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

    }

}
