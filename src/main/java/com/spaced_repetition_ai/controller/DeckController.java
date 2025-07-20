package com.spaced_repetition_ai.controller;


import com.mongodb.lang.Nullable;
import com.spaced_repetition_ai.dto.DeckResponseDTO;
import com.spaced_repetition_ai.entity.DeckEntity;
import com.spaced_repetition_ai.entity.FlashCardEntity;
import com.spaced_repetition_ai.model.DeckType;
import com.spaced_repetition_ai.model.Language;
import com.spaced_repetition_ai.service.DeckService;

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

    @PutMapping("/{id}") // Ou @PatchMapping para atualizações parciais
    public ResponseEntity<FlashCardEntity> update(@PathVariable("id") String id, @RequestBody DeckResponseDTO dto) {
        deckService.updateDeck(id, dto.description(), dto.name(), dto.targetLanguage(), dto.sourceLanguage(), dto.audioPrompt(),
                dto.imagePrompt(), dto.textPrompt(), dto.audioPath(), dto.imagePath(), dto.easeFactor());
        return ResponseEntity.ok().build();
    }

    @GetMapping
    public List<DeckEntity> getDecks() {
        return deckService.getDecks();
    }

    @PostMapping
    public void generateDeck(@RequestParam("name") String name,
                             @RequestParam("description") String description,
                             @RequestParam("targetLanguage") @Nullable Language targetLanguage,
                             @RequestParam("sourceLanguage") @Nullable Language sourceLanguage,
                             @RequestParam("audioPrompt") @Nullable String audioPrompt,
                             @RequestParam("imagePrompt") @Nullable String imagePrompt,
                             @RequestParam("textPrompt") @Nullable String textPrompt,
                             @RequestParam("audioPath") @Nullable String audioPath,
                             @RequestParam("imagePath") @Nullable String imagePath,
                             @RequestParam(value = "easeFactor", required = false)  Double easeFactor,
                             @RequestParam("generateImage") @Nullable boolean generateImage,
                             @RequestParam("generateAudio") @Nullable boolean generateAudio,
                             @RequestParam("deckType") @Nullable DeckType deckType
                             ){
            deckService.createDeck(name, description, targetLanguage, sourceLanguage, audioPrompt, imagePrompt, textPrompt, audioPath, imagePath, easeFactor, generateImage, generateAudio, deckType);
    }



    @DeleteMapping("/{deckId}")
    public void deleteDeck(@PathVariable("deckId") String deckId) {
        deckService.removerDeck(deckId);
    }

}
