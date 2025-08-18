package com.spaced_repetition_ai.service;


import com.spaced_repetition_ai.dto.FlashcardRequestDTO;
import com.spaced_repetition_ai.entity.DeckEntity;
import com.spaced_repetition_ai.entity.FlashCardEntity;
import com.spaced_repetition_ai.entity.UserEntity;
import com.spaced_repetition_ai.model.DeckType;
import com.spaced_repetition_ai.model.FlashCard;
import com.spaced_repetition_ai.model.Language;
import com.spaced_repetition_ai.repository.DeckRepository;
import com.spaced_repetition_ai.repository.FlashCardRepository;
import com.spaced_repetition_ai.repository.StandardFlashCardRepository;
import com.spaced_repetition_ai.repository.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FlashCardServiceTest {

    @Mock
    FlashCardRepository flashCardRepository;
    @Mock
    private DeckRepository deckRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private FlashCardService flashCardService;
    @Mock
    private Language language;
    @Mock
    private TextGenerationService textGenerationService;
    @Mock
    private AudioGenerationService audioGenerationService;
    @Mock
    private StandardFlashCardRepository standardFlashCardRepository;
    @Mock
    private ImageGenerationService imageGenerationService;
    private DeckEntity deckEntity;
    private UserEntity loggedInUser;
    private FlashcardRequestDTO newFlashcardRequest;

    @BeforeEach
    void setUp() throws Exception {
        loggedInUser = new UserEntity();
        loggedInUser.setId(1L);
        loggedInUser.setUsername("testuser");

        deckEntity = new DeckEntity();
        deckEntity.setId(10L);
        deckEntity.setEaseFactor(2.5);
        deckEntity.setUser(loggedInUser);

        newFlashcardRequest = new FlashcardRequestDTO();
        newFlashcardRequest.setFront("Qual a capital do Brasil?");
        newFlashcardRequest.setBack("Brasília");


    }

    private void mockLoggedInUser() {
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
        when(authentication.getName()).thenReturn("testuser");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(loggedInUser));
    }

    @Test
    void generateFlashCard_deveCriarUmFlashCard(){
        Long deckId = 10L;
        mockLoggedInUser();
        when(deckRepository.findByUserIdAndId(loggedInUser.getId(), deckId))
                .thenReturn(Optional.of(deckEntity));

        flashCardService.generateFlashCard(deckId, newFlashcardRequest);
        ArgumentCaptor<FlashCardEntity> captor = ArgumentCaptor.forClass(FlashCardEntity.class);

        verify(flashCardRepository, times(1)).save(captor.capture());

        FlashCardEntity flashCardEntity = captor.getValue();
        assertNotNull(flashCardEntity);
        assertEquals(deckEntity, flashCardEntity.getDeck());
        assertEquals(newFlashcardRequest.getFront(), flashCardEntity.getFront());
        assertEquals(newFlashcardRequest.getBack(), flashCardEntity.getBack());
        assertEquals(deckEntity.getEaseFactor(), flashCardEntity.getEaseFactor());
        assertEquals(1, flashCardEntity.getInterval());
    }

    @Test
    void generateFlashCard_DeckNaoExiste_DeveLancarRunTimeException(){
        Long deckNaoExiste = 111L;
        mockLoggedInUser();

        when(deckRepository.findByUserIdAndId(loggedInUser.getId(), deckNaoExiste))
                .thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> flashCardService.generateFlashCard(deckNaoExiste, newFlashcardRequest));

        verify(flashCardRepository, never()).save(any(FlashCardEntity.class));
    }

    @Test
    void generateFlashCard_UsuarioNaoLogado_DeveLancarRunTimeException() {
        SecurityContextHolder.clearContext();
        assertThrows(NullPointerException.class,
                () -> flashCardService.generateFlashCard(5L, newFlashcardRequest));

        verify(deckRepository, never()).findByUserIdAndId(anyLong(), anyLong());
    }

    @Test
    void generateFlashCard_QuandoDTOVazio_DeveLancarRunTimeException() {
        Long deckId = 10L;

        FlashcardRequestDTO requestDTOVazio = new FlashcardRequestDTO();
        requestDTOVazio.setFront("");
        requestDTOVazio.setBack("qualquer coisa");

        assertThrows(RuntimeException.class,
                () -> flashCardService.generateFlashCard(deckId, requestDTOVazio));

        verify(deckRepository, never()).findByUserIdAndId(anyLong(), anyLong());
        verify(flashCardRepository, never()).save(any(FlashCardEntity.class));
        verify(userRepository, never()).findByUsername(anyString());
        verify(deckRepository, never()).save(any(DeckEntity.class));
        verify(userRepository, never()).save(any(UserEntity.class));
        verify(deckRepository, never()).findByUserIdAndId(anyLong(), anyLong());
        verify(userRepository, never()).findByUsername(anyString());
    }

    @Test
    void generateAiFlashCard_DeveRetornarSucesso(){
        Long deckId = 10L;
        String prompt = "teste";
        double easeFactor = 2.5;
        boolean standardDeck = false;
        deckEntity.setEaseFactor(easeFactor);
        deckEntity.setDeckType(DeckType.LANGUAGE);
        loggedInUser.setBalance(100);
        String front = newFlashcardRequest.getFront();
        String back = newFlashcardRequest.getBack();
        FlashCard flashCard = new FlashCard();
        flashCard.setFront(front);
        flashCard.setBack(back);
        deckEntity.setSourceLanguage(Language.PORTUGUES_BRASIL);
        deckEntity.setTargetLanguage(Language.INGLES_EUA);
        deckEntity.setGenerateImage(true);
        deckEntity.setGenerateAudio(true);

        mockLoggedInUser();
        when(deckRepository.findByUserIdAndId(loggedInUser.getId(), deckId))
                .thenReturn(Optional.of(deckEntity));

        when(standardFlashCardRepository.findByPrompt(prompt)).thenReturn(null);
        when(textGenerationService.generateTextFromJson(anyString())).thenReturn(flashCard);
        when(imageGenerationService.generateImage(anyString(), eq(null))).thenReturn(List.of("/caminho/gerado/imagem.png"));
        when(audioGenerationService.generateAudio(anyString(), eq(null))).thenReturn(List.of("/caminho/gerado/audio.wav"));


        flashCardService.generateAiFlashCard(prompt, deckEntity.getId());
        ArgumentCaptor<FlashCardEntity> captor = ArgumentCaptor.forClass(FlashCardEntity.class);

        // >>> CORREÇÃO 2: A verificação (verify) vem ANTES de pegar o valor <<<
        verify(flashCardRepository).save(captor.capture());

        FlashCardEntity savedFlashCard = captor.getValue();

        // Agora, as verificações funcionarão
        assertNotNull(savedFlashCard);
        assertEquals(deckEntity, savedFlashCard.getDeck());

        FlashCardEntity flashCardEntity = captor.getValue();
        assertNotNull(flashCardEntity);
        assertEquals(deckEntity, flashCardEntity.getDeck());
        assertEquals(newFlashcardRequest.getFront(), flashCardEntity.getFront());
        assertEquals(newFlashcardRequest.getBack(), flashCardEntity.getBack());
        assertEquals(easeFactor, flashCardEntity.getEaseFactor());
        assertEquals(1, flashCardEntity.getInterval());

    }


    @Test
    void generateAiFlashCard_UsuarioNaoLogado_DeveRetornarErro(){
        Long deckId = 10L;
        SecurityContextHolder.clearContext();
        assertThrows(NullPointerException.class,
                () -> flashCardService.generateFlashCard(5L, newFlashcardRequest));

        verify(deckRepository, never()).findByUserIdAndId(anyLong(), anyLong());
    }

    @Test
    void generateAiFlashCard_PromptVazio_DeveRetornarErro(){
        Long deckId = 10L;
        String prompt = "";

        assertThrows(RuntimeException.class,
                () -> flashCardService.generateAiFlashCard(prompt, deckId));

        verify(deckRepository, never()).findByUserIdAndId(anyLong(), anyLong());
        verify(flashCardRepository, never()).save(any(FlashCardEntity.class));
        verify(userRepository, never()).findByUsername(anyString());
        verify(deckRepository, never()).save(any(DeckEntity.class));
        verify(userRepository, never()).save(any(UserEntity.class));
        verify(deckRepository, never()).findByUserIdAndId(anyLong(), anyLong());
        verify(userRepository, never()).findByUsername(anyString());
    }




}