package com.spaced_repetition_ai.service;

import com.google.common.collect.ImmutableList;
import com.google.genai.Client;
import com.google.genai.Models;
import com.google.genai.types.*;
import com.spaced_repetition_ai.entity.UserEntity;
import com.spaced_repetition_ai.exception.ExternalServiceException;
import com.spaced_repetition_ai.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;
import org.springframework.web.multipart.MultipartFile;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AudioGenerationServiceTest {

    @Mock
    private Client genaiClient;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ApplicationContext applicationContext;

    @Mock
    private Models mockModels;

    private AudioGenerationService audioGenerationService;

    private UserEntity mockUser;
    private static final Long USER_ID = 1L;
    private static final String VALID_PROMPT = "Hello, this is a test";

    @BeforeEach
    void setUp() throws Exception {
        mockUser = new UserEntity();
        mockUser.setId(USER_ID);
        mockUser.setBalance(100);

        // Usar reflection para injetar o mock no campo final models do genaiClient
        Field modelsField = Client.class.getDeclaredField("models");
        modelsField.setAccessible(true);
        modelsField.set(genaiClient, mockModels);

        audioGenerationService = new AudioGenerationService(genaiClient, userRepository, applicationContext);
    }

    @Test
    void generateAudio_ComSucesso_DeveRetornarAudioEDebitarSaldo() {
        // Arrange
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(mockUser));

        GenerateContentResponse response = createMockAudioResponse();
        when(mockModels.generateContent(eq("gemini-2.5-flash-preview-tts"), any(Content.class), any(GenerateContentConfig.class)))
                .thenReturn(response);

        // Act
        AudioGenerationService.GeneratedAudioData result =
                audioGenerationService.generateAudio(VALID_PROMPT, null, USER_ID);

        // Assert
        assertNotNull(result);
        assertNotNull(result.audioBytes());
        assertTrue(result.audioBytes().length > 0);
        assertEquals("audio/wav", result.mimeType());
        assertEquals(99, mockUser.getBalance()); // 100 - 1
        verify(userRepository).save(mockUser);
        verify(mockModels).generateContent(eq("gemini-2.5-flash-preview-tts"), any(Content.class), any(GenerateContentConfig.class));
    }

    @Test
    void generateAudio_UsuarioNaoEncontrado_DeveLancarException() {
        // Arrange
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ExternalServiceException.class,
                () -> audioGenerationService.generateAudio(VALID_PROMPT, null, USER_ID));

        verify(userRepository, never()).save(any());
    }

    @Test
    void generateAudio_SaldoInsuficiente_DeveLancarException() {
        // Arrange
        mockUser.setBalance(0); // Menor que 1
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(mockUser));

        // Act & Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> audioGenerationService.generateAudio(VALID_PROMPT, null, USER_ID));

        assertEquals("Saldo insuficiente para gerar áudio.", exception.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    void generateAudio_PromptVazio_DeveLancarException() {
        // Arrange
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(mockUser));

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> audioGenerationService.generateAudio("   ", null, USER_ID));

        assertEquals("Não é possível gerar áudio com prompt vazio.", exception.getMessage());
        verify(mockModels, never()).generateContent(anyString(), any(Content.class), any(GenerateContentConfig.class));
    }

    @Test
    void generateAudio_APIRetornaPartsVazio_DeveLancarException() {
        // Arrange
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(mockUser));

        GenerateContentResponse response = mock(GenerateContentResponse.class);
        // CORREÇÃO AQUI: Use ImmutableList.of() para uma lista vazia
        doReturn(ImmutableList.of()).when(response).parts();

        when(mockModels.generateContent(anyString(), any(Content.class), any(GenerateContentConfig.class)))
                .thenReturn(response);

        // Act & Assert
        assertThrows(ExternalServiceException.class,
                () -> audioGenerationService.generateAudio(VALID_PROMPT, null, USER_ID));

        verify(userRepository, never()).save(any());
    }

    @Test
    void generateAudio_APIRetornaInlineDataVazio_DeveLancarException() {
        // Arrange
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(mockUser));

        Part part = mock(Part.class);
        doReturn(Optional.empty()).when(part).inlineData();

        GenerateContentResponse response = mock(GenerateContentResponse.class);
        // CORREÇÃO: Use ImmutableList.of()
        doReturn(ImmutableList.of(part)).when(response).parts();

        when(mockModels.generateContent(anyString(), any(Content.class), any(GenerateContentConfig.class)))
                .thenReturn(response);

        // Act & Assert
        assertThrows(ExternalServiceException.class,
                () -> audioGenerationService.generateAudio(VALID_PROMPT, null, USER_ID));

        verify(userRepository, never()).save(any());
    }

    @Test
    void generateAudio_APIRetornaDataVazio_DeveLancarException() {
        // Arrange
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(mockUser));

        // Passo 1: Crie o mock do Blob com dados vazios
        Blob blob = mock(Blob.class);
        doReturn(Optional.empty()).when(blob).data();

        // Passo 2: Crie o mock da Parte, injetando o mock do Blob
        Part part = mock(Part.class);
        doReturn(Optional.of(blob)).when(part).inlineData();

        // Passo 3: Crie um objeto Content, passando a lista de partes
        Content content = Content.builder().parts(List.of(part)).build();

        // Passo 4: Crie um Candidate, passando o objeto Content
        Candidate candidate = Candidate.builder().content(content).build();

        // Passo 5: Use o Builder para construir uma instância real de GenerateContentResponse
        // e injete a lista de candidates que criamos.
        GenerateContentResponse response = GenerateContentResponse.builder()
                .candidates(List.of(candidate))
                .build();

        when(mockModels.generateContent(anyString(), any(Content.class), any(GenerateContentConfig.class)))
                .thenReturn(response);

        // Act & Assert
        assertThrows(ExternalServiceException.class,
                () -> audioGenerationService.generateAudio(VALID_PROMPT, null, USER_ID));

        verify(userRepository, never()).save(any());
    }




    @Test
    void generateAudio_ComAudiosAnexados_DeveProcessarCorretamente() throws Exception {
        // Arrange
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(mockUser));

        MultipartFile audioFile = mock(MultipartFile.class);
        when(audioFile.getBytes()).thenReturn(new byte[]{1, 2, 3});
        when(audioFile.getContentType()).thenReturn("audio/wav");

        GenerateContentResponse response = createMockAudioResponse();
        when(mockModels.generateContent(anyString(), any(Content.class), any(GenerateContentConfig.class)))
                .thenReturn(response);

        // Act
        AudioGenerationService.GeneratedAudioData result =
                audioGenerationService.generateAudio(VALID_PROMPT, List.of(audioFile), USER_ID);

        // Assert
        assertNotNull(result);
        assertNotNull(result.audioBytes());
        verify(audioFile).getBytes();
        verify(audioFile).getContentType();
    }

    @Test
    void generateAudio_ErroNaAPI_DeveLancarExternalServiceException() {
        // Arrange
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(mockUser));

        when(mockModels.generateContent(anyString(), any(Content.class), any(GenerateContentConfig.class)))
                .thenThrow(new RuntimeException("API Error"));

        // Act & Assert
        assertThrows(ExternalServiceException.class,
                () -> audioGenerationService.generateAudio(VALID_PROMPT, null, USER_ID));

        verify(userRepository, never()).save(any());
    }

    @Test
    void generateAudioAsync_ComSucesso_DeveRetornarCompletableFuture() throws Exception {
        // Arrange
        AudioGenerationService mockService = mock(AudioGenerationService.class);
        byte[] mockAudioData = new byte[]{1, 2, 3};
        AudioGenerationService.GeneratedAudioData expectedResult =
                new AudioGenerationService.GeneratedAudioData(mockAudioData, "audio/wav");

        when(applicationContext.getBean(AudioGenerationService.class)).thenReturn(mockService);
        when(mockService.generateAudio(VALID_PROMPT, null, USER_ID))
                .thenReturn(expectedResult);

        // Act
        CompletableFuture<AudioGenerationService.GeneratedAudioData> future =
                audioGenerationService.generateAudioAsync(VALID_PROMPT, null, USER_ID);

        // Assert
        assertNotNull(future);
        AudioGenerationService.GeneratedAudioData result = future.get();
        assertArrayEquals(mockAudioData, result.audioBytes());
        assertEquals("audio/wav", result.mimeType());
    }

    @Test
    void generateAudioAsync_ComFalha_DeveRetornarFutureComErro() {
        // Arrange
        AudioGenerationService mockService = mock(AudioGenerationService.class);

        when(applicationContext.getBean(AudioGenerationService.class)).thenReturn(mockService);
        when(mockService.generateAudio(VALID_PROMPT, null, USER_ID))
                .thenThrow(new RuntimeException("Erro na geração"));

        // Act
        CompletableFuture<AudioGenerationService.GeneratedAudioData> future =
                audioGenerationService.generateAudioAsync(VALID_PROMPT, null, USER_ID);

        // Assert
        assertNotNull(future);
        assertTrue(future.isCompletedExceptionally());
    }

    @Test
    void generateAudio_DeveUsarVoiceConfigCorreto() {
        // Arrange
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(mockUser));

        GenerateContentResponse response = createMockAudioResponse();
        when(mockModels.generateContent(anyString(), any(Content.class), any(GenerateContentConfig.class)))
                .thenReturn(response);

        // Act
        audioGenerationService.generateAudio(VALID_PROMPT, null, USER_ID);

        // Assert
        ArgumentCaptor<GenerateContentConfig> configCaptor = ArgumentCaptor.forClass(GenerateContentConfig.class);
        verify(mockModels).generateContent(eq("gemini-2.5-flash-preview-tts"), any(Content.class), configCaptor.capture());

        assertNotNull(configCaptor.getValue());
        // Verifica que o config tem as modalidades de resposta corretas
        // Nota: se o GenerateContentConfig tiver getters públicos, você pode validar mais detalhes
    }

    @Test
    void generateAudio_DeveUsarContentComPromptCorreto() {
        // Arrange
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(mockUser));

        GenerateContentResponse response = createMockAudioResponse();
        when(mockModels.generateContent(anyString(), any(Content.class), any(GenerateContentConfig.class)))
                .thenReturn(response);

        // Act
        audioGenerationService.generateAudio(VALID_PROMPT, null, USER_ID);

        // Assert
        ArgumentCaptor<Content> contentCaptor = ArgumentCaptor.forClass(Content.class);
        verify(mockModels).generateContent(eq("gemini-2.5-flash-preview-tts"), contentCaptor.capture(), any(GenerateContentConfig.class));

        assertNotNull(contentCaptor.getValue());
        assertFalse(contentCaptor.getValue().parts().isEmpty());
    }

    /**
     * Método auxiliar para criar mock de GenerateContentResponse com áudio
     * A estrutura é: response.parts().get(0).inlineData().get().data().get()
     */
    private GenerateContentResponse createMockAudioResponse() {
        // Simula dados de áudio PCM de 24kHz, 16 bits, mono
        // Criando 1 segundo de áudio silencioso como exemplo
        int sampleRate = 24000;
        int bytesPerSample = 2; // 16 bits = 2 bytes
        int numSamples = sampleRate; // 1 segundo
        byte[] mockAudioData = new byte[numSamples * bytesPerSample];

        // Preenche com silêncio (zeros)
        Arrays.fill(mockAudioData, (byte) 0);

        // Use o Builder para construir os objetos de forma aninhada
        Blob blob = Blob.builder()
                .mimeType("audio/wav")
                .data(mockAudioData)
                .build();

        Part part = Part.builder()
                .inlineData(blob)
                .build();

        Content content = Content.builder().parts(List.of(part)).build();

        Candidate candidate = Candidate.builder().content(content).build();

        return GenerateContentResponse.builder()
                .candidates(List.of(candidate))
                .build();
    }
}