package com.spaced_repetition_ai.service;

import com.spaced_repetition_ai.dto.DeckDetailsReponseDTO;
import com.spaced_repetition_ai.dto.DeckRequestDTO;
import com.spaced_repetition_ai.dto.DeckResponseDTO;
import com.spaced_repetition_ai.dto.DeckUpdateDTO;
import com.spaced_repetition_ai.entity.DeckEntity;

import com.spaced_repetition_ai.entity.UserEntity;
import com.spaced_repetition_ai.model.DeckType;
import com.spaced_repetition_ai.model.ImageStyle;
import com.spaced_repetition_ai.model.Language;
import com.spaced_repetition_ai.repository.DeckRepository;
import com.spaced_repetition_ai.repository.FlashCardRepository;
import com.spaced_repetition_ai.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DeckService {

    private final DeckRepository deckRepository;
    private final UserRepository userRepository;
    private final FlashCardRepository flashCardRepository;

    public DeckService(DeckRepository deckRepository, UserRepository userRepository, FlashCardRepository flashCardRepository) {
        this.deckRepository = deckRepository;
        this.userRepository = userRepository;
        this.flashCardRepository = flashCardRepository;
    }

    public List<DeckResponseDTO> getDecks() {
        UserEntity usuarioLogado = getUsuarioLogado();

        List<DeckEntity> decks = deckRepository.findByUserId(usuarioLogado.getId());

        return decks.stream()
                .map(DeckResponseDTO::fromEntity)
                .collect(Collectors.toList());
    }

    public DeckDetailsReponseDTO getDeckInfo(Long deckId){
        UserEntity usuarioLogado = getUsuarioLogado();

        int flashcardsToReview = flashCardRepository.findReviewableCardsByDeckAndUser(deckId, usuarioLogado.getId(), LocalDateTime.now()).size();
        int totalFlashcardsDeck = flashCardRepository.findByDeckId(deckId).size();



        DeckEntity deck = deckRepository.findByUserIdAndId(usuarioLogado.getId(), deckId)
                .orElseThrow(() -> new RuntimeException("Deck não encontrado"));

        double scoreDeck = deck.getTotalReviewCount() > 0
                ? deck.getTotalReviewRate() / (double) deck.getTotalReviewCount()
                : 0.0;

                return new DeckDetailsReponseDTO(
                        deck.getId(),
                        deck.getName(),
                        deck.getDescription(),
                        deck.getTargetLanguage(),
                        deck.getSourceLanguage(),
                        deck.getEaseFactor(),
                        deck.getGenerateImage(),
                        deck.getGenerateAudio(),
                        deck.getDeckType(),
                        flashcardsToReview,
                        totalFlashcardsDeck,
                        scoreDeck
                );




    }

    @Transactional
    public void updateDeck(Long id, DeckUpdateDTO dto) {

        UserEntity usuarioLogado = getUsuarioLogado();
        deckRepository.findByUserIdAndId(usuarioLogado.getId(), id).map(ent -> {
                    ent.setDescription(dto.getDescription());
                    ent.setName(dto.getName());
                    ent.setTargetLanguage(dto.getTargetLanguage());
                    ent.setSourceLanguage(dto.getSourceLanguage());
                    ent.setAudioPath(dto.getAudioPath());
                    ent.setImagePath(dto.getImagePath());
                    ent.setEaseFactor(dto.getEaseFactor());
            deckRepository.save(ent);
            log.info("Deck atualizado com sucesso!");
            return ent;
        }).orElseThrow(() -> new RuntimeException("Deck não encontrado ou não pertence ao usuário."));

    }


    public void removerDeck(Long deckId) {

        UserEntity usuarioLogado = getUsuarioLogado();
        DeckEntity deck = deckRepository.findByUserIdAndId(usuarioLogado.getId(), deckId)
                .orElseThrow(() -> new RuntimeException("Deck não encontrado ou não pertence ao usuário."));

        deckRepository.deleteById(deck.getId());
    }

    @Transactional
    public void createDeck(DeckRequestDTO dto){

        UserEntity usuarioLogado = getUsuarioLogado();

        if (dto.getName() == null || dto.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("O campo nome está vazio.");
        }
        if (dto.getDescription() == null || dto.getDescription().trim().isEmpty()) {
            throw new IllegalArgumentException("O campo descrição está vazio.");
        }

        DeckEntity deckEntity = new DeckEntity();

        deckEntity.setUser(usuarioLogado);
        deckEntity.setName(dto.getName());
        deckEntity.setDescription(dto.getDescription());
        deckEntity.setTargetLanguage(Optional.ofNullable(dto.getTargetLanguage()).orElse(Language.INGLES_EUA));
        deckEntity.setSourceLanguage(Optional.ofNullable(dto.getSourceLanguage()).orElse(Language.PORTUGUES_BRASIL));
        deckEntity.setEaseFactor(Optional.ofNullable(dto.getEaseFactor()).orElse(2.0));
        deckEntity.setAudioPath((dto.getAudioPath() == null || dto.getAudioPath().trim().isEmpty()) ? "Storage/" : dto.getAudioPath() + "/" );
        deckEntity.setImagePath((dto.getImagePath() == null || dto.getImagePath().trim().isEmpty()) ? "Storage/" : dto.getImagePath() + "/");
        deckEntity.setImageStyle(Optional.ofNullable(dto.getImageStyle()).orElse(ImageStyle.ANIME_STYLE));


        deckEntity.setGenerateAudio(Optional.ofNullable(dto.getGenerateAudio()).orElse(true));
        deckEntity.setGenerateImage(Optional.ofNullable(dto.getGenerateImage()).orElse(true));

        deckEntity.setDeckType(Optional.ofNullable(dto.getDeckType()).orElse(DeckType.LANGUAGE));

        deckRepository.save(deckEntity);
        log.info("Deck criado com sucesso!");

    }

    private UserEntity getUsuarioLogado() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email) // Assumindo que findByUsername agora é findByEmail
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado: " + email));
    }


}
