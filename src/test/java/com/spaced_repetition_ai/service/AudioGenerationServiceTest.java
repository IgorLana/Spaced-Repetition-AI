package com.spaced_repetition_ai.service;

import com.google.common.collect.ImmutableList;
import com.google.genai.Client;
import com.google.genai.Models;
import com.google.genai.types.*;
import com.spaced_repetition_ai.entity.UserEntity;
import com.spaced_repetition_ai.repository.UserRepository;
import com.spaced_repetition_ai.storage.AudioStorage;
import com.spaced_repetition_ai.storage.ImageStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class AudioGenerationServiceTest {

    @Mock
    private Client genaiClient;
    @Mock
    private AudioStorage audioStorage;
    @Mock
    private UserRepository userRepository;

    @Mock
    private Models mockModels;
    @Mock
    private GenerateContentResponse mockApiResponse;
    @Mock
    private Part mockPart;
    @Mock
    private Blob mockBlob;

    @InjectMocks
    private AudioGenerationService audioGenerationService;

    private UserEntity testUser;

    @BeforeEach
    void setUp() throws Exception {
        testUser = new UserEntity();
        testUser.setUsername("testuser@example.com");

        // Usa reflection para injetar o mock no campo final do genaiClient
        Field modelsField = Client.class.getDeclaredField("models");
        modelsField.setAccessible(true);
        modelsField.set(genaiClient, mockModels);

        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
        when(SecurityContextHolder.getContext().getAuthentication().getName()).thenReturn("testuser@example.com");
    }


    @Test
    void generateAudio_ComSaldoSuficiente_DeveGerarAudio() {
        testUser.setBalance(100);
        when(userRepository.findByUsername(testUser.getUsername())).thenReturn(Optional.of(testUser));
        when(mockApiResponse.parts()).thenReturn(ImmutableList.of(mockPart));
        when(mockPart.inlineData()).thenReturn(Optional.of(mockBlob));
        when(mockBlob.mimeType()).thenReturn(Optional.of("audio/mpeg"));
        when(mockBlob.data()).thenReturn(Optional.of("audio-falsa".getBytes()));

        when(mockModels.generateContent(anyString(), any(Content.class), any(GenerateContentConfig.class)))
                .thenReturn(mockApiResponse);

        String caminhoFalso = "/audio/salvas/audio-gerada.wav";
        when(audioStorage.StorageWav(anyString(), any(byte[].class))).thenReturn(caminhoFalso);

        List<String> result = audioGenerationService.generateAudio("um prompt", null);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(caminhoFalso, result.get(0));
        assertEquals(90, testUser.getBalance());
        verify(audioStorage, times(1)).StorageWav(anyString(), any(byte[].class));
    }

    @Test
    void generateAudio_ComSaldoInsuficiente_DeveRetornarMensagemDeErro() {
        testUser.setBalance(5);
        when(userRepository.findByUsername(testUser.getUsername())).thenReturn(Optional.of(testUser));

        List<String> result = audioGenerationService.generateAudio("um prompt", null);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Saldo insuficiente para gerar audio.", result.get(0));
        assertEquals(5, testUser.getBalance());
        verify(audioStorage, never()).StorageWav(anyString(), any(byte[].class));
    }


    @Test
    void generateAudio_ComPromptVazio_DeveRetornarMensagemDeErro() {
        testUser.setBalance(100);
        when(userRepository.findByUsername(testUser.getUsername())).thenReturn(Optional.of(testUser));

        List<String> result = audioGenerationService.generateAudio("", null);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Nao é possivel gerar audio com prompt vazio.", result.get(0));
        assertEquals(100, testUser.getBalance());
        verify(audioStorage, never()).StorageWav(anyString(), any(byte[].class));
    }

    @Test
    void generateAudio_ErroNaAPI_DeveRetornarMensagemDeErro() {
        testUser.setBalance(100);
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.of(testUser));

        when(mockModels.generateContent(anyString(), any(Content.class), any(GenerateContentConfig.class)))
                .thenThrow(new RuntimeException("Ocorreu um erro ao gerar a imagem. Tente novamente."));

        List<String> result = audioGenerationService.generateAudio("um prompt", new ArrayList<>());

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Ocorreu um erro ao gerar o audio. Tente novamente.", result.get(0));

        assertEquals(100, testUser.getBalance());

        verify(audioStorage, never()).StorageWav(anyString(), any(byte[].class));
    }
}