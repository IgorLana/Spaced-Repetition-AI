package com.spaced_repetition_ai.service;

import com.spaced_repetition_ai.dto.DeckRequestDTO;
import com.spaced_repetition_ai.dto.DeckResponseDTO;
import com.spaced_repetition_ai.dto.FlashcardRequestDTO;
import com.spaced_repetition_ai.entity.DeckEntity;
import com.spaced_repetition_ai.entity.FlashCardEntity;
import com.spaced_repetition_ai.entity.UserEntity;
import com.spaced_repetition_ai.model.DeckType;
import com.spaced_repetition_ai.model.Language;
import com.spaced_repetition_ai.repository.DeckRepository;
import com.spaced_repetition_ai.repository.FlashCardRepository;
import com.spaced_repetition_ai.repository.StandardFlashCardRepository;
import com.spaced_repetition_ai.repository.UserRepository;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeckServiceTest {

    @Mock
    private DeckRepository deckRepository;
    @Mock
    private UserRepository userRepository;


    @Mock
    private Language language;

    private DeckEntity deckEntity;
    private UserEntity loggedInUser;
    private DeckRequestDTO newDeckRequest;
    private DeckRequestDTO deck2;
    private DeckResponseDTO responseDTO;

    @Spy
    @InjectMocks
    private DeckService deckService;

    @BeforeEach
    void setUp() throws Exception {
        loggedInUser = new UserEntity();
        loggedInUser.setId(1L);
        loggedInUser.setUsername("testuser");

        deckEntity = new DeckEntity();
        deckEntity.setId(10L);
        deckEntity.setEaseFactor(2.5);
        deckEntity.setUser(loggedInUser);

        newDeckRequest = new DeckRequestDTO();
        newDeckRequest.setName("test deck");
        newDeckRequest.setDescription("test deck description");
        newDeckRequest.setAudioPrompt("test audio prompt");
        newDeckRequest.setImagePrompt("test image prompt");
        newDeckRequest.setTextPrompt("test text prompt");
        newDeckRequest.setAudioPath("/caminho/audio.wav");
        newDeckRequest.setImagePath("/caminho/imagem.png");
        newDeckRequest.setEaseFactor(2.5);
        newDeckRequest.setGenerateImage(true);
        newDeckRequest.setGenerateAudio(true);
        newDeckRequest.setDeckType(DeckType.LANGUAGE);
        newDeckRequest.setStandardTextPrompt("test standard text prompt");
        newDeckRequest.setTargetLanguage(Language.PORTUGUES_BRASIL);
        newDeckRequest.setSourceLanguage(Language.INGLES_EUA);


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
    void createDeck_deveCriarUmDeck(){
        // given
        mockLoggedInUser();

        // when
        deckService.createDeck(newDeckRequest);

        // then
        ArgumentCaptor<DeckEntity> captor = ArgumentCaptor.forClass(DeckEntity.class);
        verify(deckRepository).save(captor.capture());

        DeckEntity savedDeck = captor.getValue();
        assertNotNull(savedDeck);
        assertEquals("test deck", savedDeck.getName());
        assertEquals("test deck description", savedDeck.getDescription());
        assertEquals(loggedInUser, savedDeck.getUser());
        assertEquals(2.5, savedDeck.getEaseFactor());
        assertEquals("/caminho/audio.wav/", savedDeck.getAudioPath());
        assertEquals("/caminho/imagem.png/", savedDeck.getImagePath());
        assertEquals(DeckType.LANGUAGE, savedDeck.getDeckType());
        assertEquals("test standard text prompt", savedDeck.getStandardTextPrompt());
        assertEquals(Language.PORTUGUES_BRASIL, savedDeck.getTargetLanguage());
        assertEquals(Language.INGLES_EUA, savedDeck.getSourceLanguage());
    }


    @Test
    void createDeck_NameNulo_DeveRetornarErro(){
        // given
        mockLoggedInUser();

        newDeckRequest.setName(null);

        assertThrows(RuntimeException.class,
                () -> deckService.createDeck(newDeckRequest));
        verify(deckRepository, never()).save(any(DeckEntity.class));
    }

    @Test
    void createDeck_UsuarioNaoLogado_DeveLancarRunTimeException() {
        SecurityContextHolder.clearContext();
        assertThrows(NullPointerException.class,
                () -> deckService.createDeck(newDeckRequest));
        verify(deckRepository, never()).save(any(DeckEntity.class));
        verify(userRepository, never()).findByUsername(anyString());
    }

    @Test
    void getDecks_DeveListarOsDecks_RetornarSucesso(){

        mockLoggedInUser();

        DeckEntity deckEntity2 = new DeckEntity();
        deckEntity2.setId(11L);
        deckEntity2.setName("deck 2");
        deckEntity2.setUser(loggedInUser);

        DeckEntity deckEntity3 = new DeckEntity();
        deckEntity3.setId(12L);
        deckEntity3.setName("deck 3");
        deckEntity3.setUser(loggedInUser);

        when(deckRepository.findByUserId(loggedInUser.getId()))
                .thenReturn(List.of(deckEntity, deckEntity2, deckEntity3));

        List<DeckResponseDTO> decks = deckService.getDecks();

        assertEquals("deck 2", decks.get(1).name());
        assertEquals("deck 3", decks.get(2).name());
        assertEquals(3, decks.size());

    }

}