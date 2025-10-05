package com.spaced_repetition_ai.service;

import com.spaced_repetition_ai.dto.FlashcardRequestDTO;
import com.spaced_repetition_ai.dto.FlashcardResponseDTO;
import com.spaced_repetition_ai.entity.DeckEntity;
import com.spaced_repetition_ai.entity.FlashCardEntity;
import com.spaced_repetition_ai.entity.StandardFlashCardEntity;
import com.spaced_repetition_ai.entity.UserEntity;
import com.spaced_repetition_ai.exception.DatabaseException;
import com.spaced_repetition_ai.exception.NotFoundException;
import com.spaced_repetition_ai.model.*;
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

import java.util.Base64;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FlashCardServiceTest {

    @Mock
    private TextGenerationService textGenerationService;

    @Mock
    private ImageGenerationService imageGenerationService;

    @Mock
    private AudioGenerationService audioGenerationService;

    @Mock
    private FlashCardRepository flashCardRepository;

    @Mock
    private DeckRepository deckRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private StandardFlashCardRepository standardFlashCardRepository;

    @Mock
    private AwsService awsService;

    @InjectMocks
    private FlashCardService flashCardService;

    private UserEntity mockUser;
    private DeckEntity mockDeck;
    private static final Long USER_ID = 1L;
    private static final Long DECK_ID = 1L;
    private static final Long FLASHCARD_ID = 1L;
    private static final String USER_EMAIL = "test@example.com";

    @BeforeEach
    void setUp() {
        mockUser = new UserEntity();
        mockUser.setId(USER_ID);
        mockUser.setEmail(USER_EMAIL);
        mockUser.setBalance(100);

        mockDeck = new DeckEntity();
        mockDeck.setId(DECK_ID);
        mockDeck.setUser(mockUser);
        mockDeck.setDeckType(DeckType.LANGUAGE);
        mockDeck.setImageStyle(ImageStyle.ANIME_STYLE);
        mockDeck.setSourceLanguage(Language.PORTUGUES_BRASIL);
        mockDeck.setTargetLanguage(Language.INGLES_EUA);
        mockDeck.setGenerateImage(true);
        mockDeck.setGenerateAudio(true);
        mockDeck.setEaseFactor(2.5);

        setupSecurityContext();
    }

    private void setupSecurityContext() {
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn(USER_EMAIL);
        SecurityContextHolder.setContext(securityContext);

    }

    // ========== Testes para deleteFlashCard ==========

    @Test
    void deleteFlashCard_ComSucesso_DeveDeletarFlashCard() {
        // Arrange
        FlashCardEntity flashCard = new FlashCardEntity();
        flashCard.setId(FLASHCARD_ID);
        flashCard.setDeck(mockDeck);

        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(mockUser));
        when(flashCardRepository.findByIdAndDeckUserId(FLASHCARD_ID, USER_ID))
                .thenReturn(Optional.of(flashCard));

        // Act
        flashCardService.deleteFlashCard(FLASHCARD_ID);

        // Assert
        verify(flashCardRepository).delete(flashCard);
    }

    @Test
    void deleteFlashCard_FlashCardNaoEncontrado_DeveLancarException() {
        // Arrange
        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(mockUser));
        when(flashCardRepository.findByIdAndDeckUserId(FLASHCARD_ID, USER_ID))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(NotFoundException.class,
                () -> flashCardService.deleteFlashCard(FLASHCARD_ID));

        verify(flashCardRepository, never()).delete(any());
    }

    @Test
    void deleteFlashCard_ErroAoDeletar_DeveLancarDatabaseException() {
        // Arrange
        FlashCardEntity flashCard = new FlashCardEntity();
        flashCard.setId(FLASHCARD_ID);
        flashCard.setDeck(mockDeck);

        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(mockUser));
        when(flashCardRepository.findByIdAndDeckUserId(FLASHCARD_ID, USER_ID))
                .thenReturn(Optional.of(flashCard));
        doThrow(new RuntimeException("Database error")).when(flashCardRepository).delete(any());

        // Act & Assert
        assertThrows(DatabaseException.class,
                () -> flashCardService.deleteFlashCard(FLASHCARD_ID));
    }

    // ========== Testes para updateFlashCard ==========

    @Test
    void updateFlashCard_ComSucesso_DeveAtualizarFlashCard() {
        // Arrange
        FlashCardEntity flashCard = new FlashCardEntity();
        flashCard.setId(FLASHCARD_ID);
        flashCard.setDeck(mockDeck);

        FlashcardRequestDTO dto = new FlashcardRequestDTO();
        dto.setFront("Updated Front");
        dto.setBack("Updated Back");
        dto.setImagePath("/image/path");
        dto.setAudioPath("/audio/path");

        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(mockUser));
        when(flashCardRepository.findByIdAndDeckUserId(FLASHCARD_ID, USER_ID))
                .thenReturn(Optional.of(flashCard));

        // Act
        flashCardService.updateFlashCard(FLASHCARD_ID, dto);

        // Assert
        verify(flashCardRepository).save(flashCard);
        assertEquals("Updated Front", flashCard.getFront());
        assertEquals("Updated Back", flashCard.getBack());
        assertEquals("/image/path", flashCard.getImagePath());
        assertEquals("/audio/path", flashCard.getAudioPath());
    }

    @Test
    void updateFlashCard_FlashCardNaoEncontrado_DeveLancarException() {
        // Arrange
        FlashcardRequestDTO dto = new FlashcardRequestDTO();
        dto.setFront("Front");
        dto.setBack("Back");

        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(mockUser));
        when(flashCardRepository.findByIdAndDeckUserId(FLASHCARD_ID, USER_ID))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(NotFoundException.class,
                () -> flashCardService.updateFlashCard(FLASHCARD_ID, dto));
    }

    // ========== Testes para generateFlashCard ==========

    @Test
    void generateFlashCard_ComSucesso_DeveSalvarFlashCard() {
        // Arrange
        FlashcardRequestDTO dto = new FlashcardRequestDTO();
        dto.setFront("Front text");
        dto.setBack("Back text");
        dto.setImagePath("/image");
        dto.setAudioPath("/audio");

        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(mockUser));
        when(deckRepository.findByUserIdAndId(USER_ID, DECK_ID))
                .thenReturn(Optional.of(mockDeck));

        // Act
        flashCardService.generateFlashCard(DECK_ID, dto);

        // Assert
        ArgumentCaptor<FlashCardEntity> captor = ArgumentCaptor.forClass(FlashCardEntity.class);
        verify(flashCardRepository).save(captor.capture());

        FlashCardEntity savedCard = captor.getValue();
        assertEquals("Front text", savedCard.getFront());
        assertEquals("Back text", savedCard.getBack());
        assertNotNull(savedCard.getCreatedDate());
        assertNotNull(savedCard.getNextReview());
    }

    @Test
    void generateFlashCard_FrontVazio_DeveLancarException() {
        // Arrange
        FlashcardRequestDTO dto = new FlashcardRequestDTO();
        dto.setFront("   ");
        dto.setBack("Back text");

        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(mockUser));

        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> flashCardService.generateFlashCard(DECK_ID, dto));

        verify(flashCardRepository, never()).save(any());
    }

    @Test
    void generateFlashCard_DeckNaoEncontrado_DeveLancarException() {
        // Arrange
        FlashcardRequestDTO dto = new FlashcardRequestDTO();
        dto.setFront("Front");
        dto.setBack("Back");

        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(mockUser));
        when(deckRepository.findByUserIdAndId(USER_ID, DECK_ID))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class,
                () -> flashCardService.generateFlashCard(DECK_ID, dto));
    }

    // ========== Testes para generateAiFlashCard ==========

    @Test
    void generateAiFlashCard_ComCacheHit_DeveRetornarFlashCardPadrao() throws Exception {
        // Arrange
        String prompt = "Test Prompt";
        StandardFlashCardEntity cachedCard = new StandardFlashCardEntity();
        cachedCard.setFront("Cached Front");
        cachedCard.setBack("Cached Back");
        cachedCard.setImagePath("/cached/image");
        cachedCard.setAudioPath("/cached/audio");

        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(mockUser));
        when(deckRepository.findByUserIdAndId(USER_ID, DECK_ID))
                .thenReturn(Optional.of(mockDeck));
        when(standardFlashCardRepository.findByPromptAndImageStyleAndSourceLanguageAndTargetLanguage(
                anyString(), any(), any(), any()))
                .thenReturn(Optional.of(cachedCard));
        when(awsService.downloadFileAsBase64("/cached/image")).thenReturn("imageBase64");
        when(awsService.downloadFileAsBase64("/cached/audio")).thenReturn("audioBase64");

        // Act
        CompletableFuture<FlashcardResponseDTO> future =
                flashCardService.generateAiFlashCard(prompt, DECK_ID);
        FlashcardResponseDTO result = future.get();

        // Assert
        assertNotNull(result);
        assertEquals("Cached Front", result.getFront());
        assertEquals("Cached Back", result.getBack());
        assertEquals("imageBase64", result.getImage());
        assertEquals("audioBase64", result.getAudio());
    }

    @Test
    void generateAiFlashCard_SemCache_DeveGerarNovoFlashCard() throws Exception {
        // Arrange
        String prompt = "New Prompt";
        FlashCard textCard = new FlashCard();
        textCard.setFront("Generated Front");
        textCard.setBack("Generated Back");
        byte[] imageBytes = "image".getBytes();
        byte[] audioBytes = "audio".getBytes();

        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(mockUser));
        when(deckRepository.findByUserIdAndId(USER_ID, DECK_ID))
                .thenReturn(Optional.of(mockDeck));
        when(standardFlashCardRepository.findByPromptAndImageStyleAndSourceLanguageAndTargetLanguage(
                anyString(), any(), any(), any()))
                .thenReturn(Optional.empty());

        when(textGenerationService.generateTextFromJsonAsync(anyString()))
                .thenReturn(CompletableFuture.completedFuture(textCard));

        // Usar lenient() ou anyString() para evitar strict stubbing
        lenient().when(imageGenerationService.generateImageAsync(anyString(), any(), any(), anyLong()))
                .thenReturn(CompletableFuture.completedFuture(
                        new ImageGenerationService.GeneratedImageData(imageBytes, "image/png")));

        lenient().when(audioGenerationService.generateAudioAsync(anyString(), any(), anyLong()))
                .thenReturn(CompletableFuture.completedFuture(
                        new AudioGenerationService.GeneratedAudioData(audioBytes, "audio/wav")));

        // Act
        CompletableFuture<FlashcardResponseDTO> future =
                flashCardService.generateAiFlashCard(prompt, DECK_ID);
        FlashcardResponseDTO result = future.get();

        // Assert
        assertNotNull(result);
        assertEquals("Generated Front", result.getFront());
        assertEquals("Generated Back", result.getBack());
        assertNotNull(result.getImage());
        assertNotNull(result.getAudio());
    }


    @Test
    void generateAiFlashCard_PromptVazio_DeveLancarException() {
        // Arrange
        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(mockUser));
        when(deckRepository.findByUserIdAndId(USER_ID, DECK_ID))
                .thenReturn(Optional.of(mockDeck));

        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> flashCardService.generateAiFlashCard("  ", DECK_ID));
    }

    @Test
    void generateAiFlashCard_SemSaldoParaImagem_NaoDeveGerarImagem() throws Exception {
        // Arrange
        String prompt = "Test";
        mockUser.setBalance(3); // Menos que 5
        FlashCard textCard = new FlashCard();
        textCard.setFront("Generated Front");
        textCard.setBack("Generated Back");

        byte[] audioBytes = "audio".getBytes();

        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(mockUser));
        when(deckRepository.findByUserIdAndId(USER_ID, DECK_ID))
                .thenReturn(Optional.of(mockDeck));
        when(standardFlashCardRepository.findByPromptAndImageStyleAndSourceLanguageAndTargetLanguage(
                anyString(), any(), any(), any()))
                .thenReturn(Optional.empty());
        when(textGenerationService.generateTextFromJsonAsync(anyString()))
                .thenReturn(CompletableFuture.completedFuture(textCard));
        lenient().when(audioGenerationService.generateAudioAsync(anyString(), any(), anyLong()))
                .thenReturn(CompletableFuture.completedFuture(
                        new AudioGenerationService.GeneratedAudioData(audioBytes, "audio/wav")));

        // Act
        CompletableFuture<FlashcardResponseDTO> future =
                flashCardService.generateAiFlashCard(prompt, DECK_ID);
        FlashcardResponseDTO result = future.get();

        // Assert
        assertNotNull(result);
        assertNull(result.getImage());
        verify(imageGenerationService, never()).generateImageAsync(anyString(), any(), any(), anyLong());
    }

    @Test
    void generateAiFlashCard_SemSaldoParaAudio_NaoDeveGerarAudio() throws Exception {
        // Arrange
        String prompt = "Test";
        mockUser.setBalance(0); // Sem saldo
        FlashCard textCard = new FlashCard();

        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(mockUser));
        when(deckRepository.findByUserIdAndId(USER_ID, DECK_ID))
                .thenReturn(Optional.of(mockDeck));
        when(standardFlashCardRepository.findByPromptAndImageStyleAndSourceLanguageAndTargetLanguage(
                anyString(), any(), any(), any()))
                .thenReturn(Optional.empty());
        when(textGenerationService.generateTextFromJsonAsync(anyString()))
                .thenReturn(CompletableFuture.completedFuture(textCard));

        // Act
        CompletableFuture<FlashcardResponseDTO> future =
                flashCardService.generateAiFlashCard(prompt, DECK_ID);
        FlashcardResponseDTO result = future.get();

        // Assert
        assertNotNull(result);
        assertNull(result.getAudio());
        verify(audioGenerationService, never()).generateAudioAsync(anyString(), any(), anyLong());
    }


    // ========== Testes para getUsuarioLogado ==========

    @Test
    void getUsuarioLogado_ComSucesso_DeveRetornarUsuario() {
        // Arrange
        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(mockUser));

        // Act
        UserEntity result = flashCardService.getUsuarioLogado();

        // Assert
        assertNotNull(result);
        assertEquals(USER_EMAIL, result.getEmail());
    }

    @Test
    void getUsuarioLogado_UsuarioNaoEncontrado_DeveLancarException() {
        // Arrange
        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(org.springframework.security.core.userdetails.UsernameNotFoundException.class,
                () -> flashCardService.getUsuarioLogado());
    }
}