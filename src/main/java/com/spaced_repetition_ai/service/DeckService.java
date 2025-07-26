package com.spaced_repetition_ai.service;

import com.spaced_repetition_ai.dto.DeckRequestDTO;
import com.spaced_repetition_ai.dto.DeckResponseDTO;
import com.spaced_repetition_ai.dto.DeckUpdateDTO;
import com.spaced_repetition_ai.entity.DeckEntity;

import com.spaced_repetition_ai.model.DeckType;
import com.spaced_repetition_ai.model.Language;
import com.spaced_repetition_ai.repository.DeckRepository;
import com.spaced_repetition_ai.util.DefaultPrompts;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


@Service
public class DeckService {

    private final DeckRepository deckRepository;

    public DeckService(DeckRepository deckRepository) {
        this.deckRepository = deckRepository;
    }

    public List<DeckResponseDTO> getDecks() {
        List<DeckEntity> decks = deckRepository.findAll();
        System.out.println("Encontrados " + decks.size() + " decks.");
        return decks.stream()
                .map(DeckResponseDTO::fromEntity)
                .collect(Collectors.toList());
    }


    public void updateDeck(String id, DeckUpdateDTO dto) {


        deckRepository.findById(id).map(ent -> {
                    ent.setDescription(dto.getDescription());
                    ent.setName(dto.getName());
                    ent.setTargetLanguage(dto.getTargetLanguage());
                    ent.setSourceLanguage(dto.getSourceLanguage());
                    ent.setAudioPrompt(dto.getAudioPrompt());
                    ent.setImagePrompt(dto.getImagePrompt());
                    ent.setTextPrompt(dto.getTextPrompt());
                    ent.setAudioPath(dto.getAudioPath());
                    ent.setImagePath(dto.getImagePath());
                    ent.setEaseFactor(dto.getEaseFactor());
            deckRepository.save(ent);
            System.out.println("Deck atualizado com sucesso!");
            return ent;
        });

    }


    public void removerDeck(String deckId) {
    deckRepository.deleteById(deckId);
    }


    public void createDeck(DeckRequestDTO dto){
        DeckEntity deckEntity = new DeckEntity();

        deckEntity.setName(dto.getName());
        deckEntity.setDescription(dto.getDescription());
        deckEntity.setTargetLanguage(Optional.ofNullable(dto.getTargetLanguage()).orElse(Language.INGLES_EUA));
        deckEntity.setSourceLanguage(Optional.ofNullable(dto.getSourceLanguage()).orElse(Language.PORTUGUES_BRASIL));
        deckEntity.setEaseFactor(Optional.ofNullable(dto.getEaseFactor()).orElse(2.0));
        deckEntity.setAudioPath((dto.getAudioPath() == null || dto.getAudioPath().trim().isEmpty()) ? "Storage/" : dto.getAudioPath() + "/" );
        deckEntity.setImagePath((dto.getImagePath() == null || dto.getImagePath().trim().isEmpty()) ? "Storage/" : dto.getImagePath() + "/");
        deckEntity.setTextPrompt((dto.getTextPrompt() == null || dto.getTextPrompt().trim().isEmpty()) ? DefaultPrompts.DEFAULT_TEXT_PROMPT_LANGUAGE : dto.getTextPrompt());
        deckEntity.setStandardTextPrompt((dto.getStandardTextPrompt() == null || dto.getStandardTextPrompt().trim().isEmpty()) ? DefaultPrompts.DEFAULT_TEXT_PROMPT : dto.getStandardTextPrompt());
        deckEntity.setImagePrompt((dto.getImagePrompt() == null || dto.getImagePrompt().trim().isEmpty()) ? DefaultPrompts.DEFAULT_IMAGE_PROMPT : dto.getImagePrompt());
        deckEntity.setAudioPrompt(Optional.ofNullable(dto.getAudioPrompt()).orElse(""));


        deckEntity.setGenerateAudio(Optional.ofNullable(dto.getGenerateAudio()).orElse(true));
        deckEntity.setGenerateImage(Optional.ofNullable(dto.getGenerateImage()).orElse(true));

        deckEntity.setDeckType(Optional.ofNullable(dto.getDeckType()).orElse(DeckType.LANGUAGE));

        deckRepository.save(deckEntity);
        System.out.println("Deck criado com sucesso!");

    }




}
