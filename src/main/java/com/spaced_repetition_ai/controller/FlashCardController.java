package com.spaced_repetition_ai.controller;

import com.spaced_repetition_ai.dto.FlashcardRequestDTO;
import com.spaced_repetition_ai.dto.FlashcardResponseDTO;
import com.spaced_repetition_ai.entity.FlashCardEntity;
import com.spaced_repetition_ai.entity.UserEntity;
import com.spaced_repetition_ai.service.AwsService;
import com.spaced_repetition_ai.service.FlashCardService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import java.util.concurrent.ExecutionException;


@Slf4j
@RestController
@RequestMapping("/api/flashcard")
public class FlashCardController {

    private final FlashCardService flashCardService;
    private final AwsService awsService;

    public FlashCardController(FlashCardService flashCardService, AwsService awsService) {
        this.awsService = awsService;
        this.flashCardService = flashCardService;
    }

    @DeleteMapping("/{flashCardId}")
    public void deleteDeck(@PathVariable("flashCardId") Long flashCardId) {
        flashCardService.deleteFlashCard(flashCardId);
    }


    @PutMapping("/{id}") // Ou @PatchMapping para atualizações parciais
    public ResponseEntity<FlashCardEntity> update(@PathVariable("id") Long id, @RequestBody FlashcardRequestDTO dto) {
        flashCardService.updateFlashCard(id, dto);
        return ResponseEntity.ok().build();
    }


    @PostMapping
    public void generateFlashCard(@RequestParam("deckId") Long deckId, @RequestBody @Valid FlashcardRequestDTO dto) {
        flashCardService.generateFlashCard(deckId, dto);

    }

    @PostMapping("/ai")
    public ResponseEntity<FlashcardResponseDTO> generateAiFlashCard(@RequestParam("prompt") String prompt, @RequestParam("deckId") Long deckId) {
        log.info("Recebida requisição para gerar flashcard com IA.");

        prompt = sanitizePrompt(prompt);

        try {
            CompletableFuture<FlashcardResponseDTO> future = flashCardService.generateAiFlashCard(prompt, deckId);

            FlashcardResponseDTO result = future.get(); // ou .join()

            log.info("Lógica assíncrona concluída. Enviando resposta.");
            return ResponseEntity.ok(result);

        } catch (InterruptedException | ExecutionException e) {
            log.error("Erro ao esperar pelo resultado do CompletableFuture", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } catch (Exception e) {
            log.error("Erro inesperado na camada do controller", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

    }

    @GetMapping("/upload-url")
    public ResponseEntity<Map<String, String>> getUploadUrl(@RequestParam("fileName") String fileName) {
        UserEntity usuarioLogado = flashCardService.getUsuarioLogado();
        String presignedUrl = awsService.generatePresignedUploadUrl(usuarioLogado.getId(), fileName);
        return ResponseEntity.ok(Map.of("url", presignedUrl));
    }


    private String sanitizePrompt(String prompt) {
        // Remove potentially harmful characters/patterns
        return prompt.replaceAll("[<>\"']", "").trim();
    }

    @GetMapping("/{id}")
    public FlashcardResponseDTO getFlashCards(@PathVariable("id") Long flashCardId){
        return flashCardService.getFlashCardById(flashCardId);
    }




}


