package com.spaced_repetition_ai.controller;


import com.mongodb.lang.Nullable;
import com.spaced_repetition_ai.service.DeckService;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/deck")
public class DeckController {

    private final DeckService deckService;

    public DeckController(DeckService deckService) {
        this.deckService = deckService;
    }

    @PostMapping
    public void generateDeck(@RequestParam("name") String name,
                             @RequestParam("description") String description,
                             @RequestParam("targetLanguage") @Nullable String targetLanguage,
                             @RequestParam("sourceLanguage") @Nullable String sourceLanguage,
                             @RequestParam("audioPrompt") @Nullable String audioPrompt,
                             @RequestParam("imagePrompt") @Nullable String imagePrompt,
                             @RequestParam("textPrompt") @Nullable String textPrompt,
                             @RequestParam("audioPath") @Nullable String audioPath,
                             @RequestParam("imagePath") @Nullable String imagePath,
                             @RequestParam(value = "easeFactor", required = false)  Double easeFactor
                             ){
            deckService.createDeck(name, description, targetLanguage, sourceLanguage, audioPrompt, imagePrompt, textPrompt, audioPath, imagePath, easeFactor);


    }
}
