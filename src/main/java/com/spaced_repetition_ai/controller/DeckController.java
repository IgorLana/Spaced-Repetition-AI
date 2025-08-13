package com.spaced_repetition_ai.controller;


import com.spaced_repetition_ai.dto.DeckRequestDTO;
import com.spaced_repetition_ai.dto.DeckResponseDTO;
import com.spaced_repetition_ai.dto.DeckUpdateDTO;
import com.spaced_repetition_ai.entity.FlashCardEntity;

import com.spaced_repetition_ai.service.DeckService;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/deck")
public class DeckController {

    private final DeckService deckService;


    public DeckController(DeckService deckService) {
        this.deckService = deckService;
    }

    @PutMapping("/{id}")
    public ResponseEntity<FlashCardEntity> update(@PathVariable("id") Long id, @RequestBody @Valid DeckUpdateDTO dto) {
        deckService.updateDeck(id, dto);
        return ResponseEntity.ok().build();
    }

    @GetMapping
    public ResponseEntity<List<DeckResponseDTO>> getDecks() {
        List<DeckResponseDTO> decks = deckService.getDecks();
        return ResponseEntity.ok(decks);
    }

    @PostMapping
    public void generateDeck(@RequestBody @Valid DeckRequestDTO dto){
            deckService.createDeck(dto);
    }



    @DeleteMapping("/{deckId}")
    public void deleteDeck(@PathVariable("deckId") Long deckId) {
        deckService.removerDeck(deckId);
    }

}
