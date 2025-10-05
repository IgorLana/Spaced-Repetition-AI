package com.spaced_repetition_ai.service;

import com.spaced_repetition_ai.entity.DeckEntity;
import com.spaced_repetition_ai.entity.StandardFlashCardEntity;
import com.spaced_repetition_ai.entity.UserEntity;
import com.spaced_repetition_ai.model.DeckType;
import com.spaced_repetition_ai.model.ImageStyle;
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

import java.util.Base64;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SaveStandartFlashCardsTest {

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

    }


    @Test
    void saveStandardFlashCards_ComSucesso_DeveSalvarNoS3EBanco() {
        // Arrange
        String prompt = "test";
        String imageBase64 = Base64.getEncoder().encodeToString("image".getBytes());
        String audioBase64 = Base64.getEncoder().encodeToString("audio".getBytes());

        when(standardFlashCardRepository.findByPromptAndImageStyleAndSourceLanguageAndTargetLanguage(
                anyString(), any(), any(), any()))
                .thenReturn(Optional.empty());
        when(awsService.uploadPublicMedia(any(byte[].class), eq("image"), anyString()))
                .thenReturn("/s3/image/path");
        when(awsService.uploadPublicMedia(any(byte[].class), eq("audio"), anyString()))
                .thenReturn("/s3/audio/path");

        // Act
        flashCardService.saveStandardFlashCards(
                prompt, "Front", "Back", imageBase64, audioBase64,
                ImageStyle.ANIME_STYLE, Language.INGLES_EUA, Language.PORTUGUES_BRASIL);

        // Assert
        ArgumentCaptor<StandardFlashCardEntity> captor = ArgumentCaptor.forClass(StandardFlashCardEntity.class);
        verify(standardFlashCardRepository).save(captor.capture());

        StandardFlashCardEntity saved = captor.getValue();
        assertEquals("test", saved.getPrompt());
        assertEquals("/s3/image/path", saved.getImagePath());
        assertEquals("/s3/audio/path", saved.getAudioPath());
    }

    @Test
    void saveStandardFlashCards_JaExiste_NaoDeveSalvar() {
        // Arrange
        StandardFlashCardEntity existing = new StandardFlashCardEntity();


        // Act
        flashCardService.saveStandardFlashCards(
                "test", "Front", "Back", null, null,
                ImageStyle.ANIME_STYLE, Language.INGLES_EUA, Language.PORTUGUES_BRASIL);

        // Assert
        verify(standardFlashCardRepository, never()).save(any());
        verify(awsService, never()).uploadPublicMedia(any(), anyString(), anyString());
    }

    @Test
    void saveStandardFlashCards_PromptVazio_NaoDeveSalvar() {
        // Act
        flashCardService.saveStandardFlashCards(
                "  ", "Front", "Back", null, null,
                ImageStyle.ANIME_STYLE, Language.INGLES_EUA, Language.PORTUGUES_BRASIL);

        // Assert
        verify(standardFlashCardRepository, never()).save(any());
    }


}
