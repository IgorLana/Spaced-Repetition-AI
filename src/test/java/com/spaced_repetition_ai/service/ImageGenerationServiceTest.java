package com.spaced_repetition_ai.service;


import com.google.genai.Client;
import com.google.genai.Models;
import com.google.genai.types.*;
import com.spaced_repetition_ai.entity.UserEntity;
import com.spaced_repetition_ai.exception.ExternalServiceException;
import com.spaced_repetition_ai.model.ImageStyle;
import com.spaced_repetition_ai.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;



import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ImageGenerationServiceTest {

    @Mock
    private Client genaiClient;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ApplicationContext applicationContext;

    @Mock
    private Models mockModels;
    @Mock
    private GenerateContentResponse mockApiResponse;
    @Mock
    private Part mockPart;
    @Mock
    private Blob mockBlob;

    @InjectMocks
    private ImageGenerationService imageGenerationService;

    private UserEntity mockUser;
    private static final Long USER_ID = 1L;
    private static final String VALID_PROMPT = "test prompt";

    @BeforeEach
    void setUp() throws Exception {
        mockUser = new UserEntity();
        mockUser.setId(USER_ID);
        mockUser.setBalance(100);

        UserEntity testeUser = new UserEntity();
        testeUser.setId(2L);
        testeUser.setBalance(100);

        // Usar reflection para injetar o mock no campo final models do genaiClient
        Field modelsField = Client.class.getDeclaredField("models");
        modelsField.setAccessible(true);
        modelsField.set(genaiClient, mockModels);

        imageGenerationService = new ImageGenerationService(genaiClient, userRepository, applicationContext);
    }

    @Test
    void generateImage_ComSucesso_DeveRetornarImagemEDebitarSaldo() throws Exception {
        // Arrange
        ImageStyle style = mock(ImageStyle.class);
        when(style.getTemplate()).thenReturn("Generate image for ${word} in ${language}");

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(mockUser));

        // Mock da resposta do Gemini para geração de texto
        GenerateContentResponse contentResponse = mock(GenerateContentResponse.class);
        when(contentResponse.text()).thenReturn("Enhanced prompt for image generation");
        when(mockModels.generateContent(eq("gemini-2.5-flash"), anyString(), isNull()))
                .thenReturn(contentResponse);

        // Mock da resposta de geração de imagens
        byte[] mockImageData = new byte[]{1, 2, 3, 4, 5};
        GenerateImagesResponse imagesResponse = createMockImageResponse(mockImageData);

        when(mockModels.generateImages(eq("imagen-4.0-fast-generate-001"), anyString(), any(GenerateImagesConfig.class)))
                .thenReturn(imagesResponse);

        // Act
        ImageGenerationService.GeneratedImageData result =
                imageGenerationService.generateImage(VALID_PROMPT, null, style, USER_ID);

        // Assert
        assertNotNull(result);
        assertArrayEquals(mockImageData, result.imageBytes());
        assertEquals("image/png", result.mimeType());
        assertEquals(95, mockUser.getBalance()); // 100 - 5
        verify(userRepository).save(mockUser);
        verify(mockModels).generateImages(eq("imagen-4.0-fast-generate-001"), anyString(), any(GenerateImagesConfig.class));
    }

    @Test
    void naoDeveGerarImagem_QuandoSaldoInsuficiente() {
        UserEntity testUser1 = new UserEntity();
        testUser1.setId(2L);
        testUser1.setBalance(4);
        when(userRepository.findById(testUser1.getId())).thenReturn(Optional.of(testUser1));

        // Act & Assert
        // A exceção esperada é IllegalStateException, não UsernameNotFoundException
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> imageGenerationService.generateImage("um prompt", null, ImageStyle.ANIME_STYLE, testUser1.getId()));

        assertEquals("Saldo insuficiente para gerar imagem.", exception.getMessage());

        // Verificamos se o saldo não foi alterado
        assertEquals(4, testUser1.getBalance());

        // Verificamos se o método save NÃO foi chamado
        verify(userRepository, never()).save(any(UserEntity.class));

    }


    @Test
    void generateImage_UsuarioNaoEncontrado_DeveLancarException() {
        // Arrange
        ImageStyle style = mock(ImageStyle.class);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ExternalServiceException.class,
                () -> imageGenerationService.generateImage(VALID_PROMPT, null, style, USER_ID));

        verify(userRepository, never()).save(any());
    }

    @Test
    void generateImage_SaldoInsuficiente_DeveLancarException() {
        // Arrange
        mockUser.setBalance(3); // Menor que 5
        ImageStyle style = mock(ImageStyle.class);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(mockUser));

        // Act & Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> imageGenerationService.generateImage(VALID_PROMPT, null, style, USER_ID));

        assertEquals("Saldo insuficiente para gerar imagem.", exception.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    void generateImage_PromptVazio_DeveLancarException() {
        // Arrange
        ImageStyle style = mock(ImageStyle.class);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(mockUser));

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> imageGenerationService.generateImage("   ", null, style, USER_ID));

        assertEquals("Não é possível gerar imagem com prompt vazio.", exception.getMessage());
        verify(mockModels, never()).generateImages(anyString(), anyString(), any());
    }

    @Test
    void generateImage_APIRetornaListaVazia_DeveLancarException() throws Exception {
        // Arrange
        ImageStyle style = mock(ImageStyle.class);
        when(style.getTemplate()).thenReturn("Template: ${word}");
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(mockUser));

        GenerateContentResponse contentResponse = mock(GenerateContentResponse.class);
        when(contentResponse.text()).thenReturn("prompt");
        when(mockModels.generateContent(anyString(), anyString(), isNull()))
                .thenReturn(contentResponse);

        GenerateImagesResponse imagesResponse = mock(GenerateImagesResponse.class);
        when(imagesResponse.generatedImages()).thenReturn(Optional.of(Collections.emptyList()));
        when(mockModels.generateImages(anyString(), anyString(), any(GenerateImagesConfig.class)))
                .thenReturn(imagesResponse);

        // Act & Assert
        assertThrows(ExternalServiceException.class,
                () -> imageGenerationService.generateImage(VALID_PROMPT, null, style, USER_ID));

        verify(userRepository, never()).save(any());
    }

    @Test
    void generateImage_APIRetornaOptionalEmpty_DeveLancarException() throws Exception {
        // Arrange
        ImageStyle style = mock(ImageStyle.class);
        when(style.getTemplate()).thenReturn("Template: ${word}");
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(mockUser));

        GenerateContentResponse contentResponse = mock(GenerateContentResponse.class);
        when(contentResponse.text()).thenReturn("prompt");
        when(mockModels.generateContent(anyString(), anyString(), isNull()))
                .thenReturn(contentResponse);

        GenerateImagesResponse imagesResponse = mock(GenerateImagesResponse.class);
        when(imagesResponse.generatedImages()).thenReturn(Optional.empty());
        when(mockModels.generateImages(anyString(), anyString(), any(GenerateImagesConfig.class)))
                .thenReturn(imagesResponse);

        // Act & Assert
        assertThrows(ExternalServiceException.class,
                () -> imageGenerationService.generateImage(VALID_PROMPT, null, style, USER_ID));

        verify(userRepository, never()).save(any());
    }

    @Test
    void generateImage_ComJSON_DeveProcessarCorretamente() throws Exception {
        // Arrange
        ImageStyle style = mock(ImageStyle.class);
        when(style.getTemplate()).thenReturn("${word} in ${language}");
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(mockUser));

        GenerateContentResponse contentResponse = mock(GenerateContentResponse.class);
        when(contentResponse.text()).thenReturn("```json\n{\"prompt\": \"test\"}\n```");
        when(mockModels.generateContent(anyString(), anyString(), isNull()))
                .thenReturn(contentResponse);

        byte[] mockImageData = new byte[]{1, 2, 3};
        GenerateImagesResponse imagesResponse = createMockImageResponse(mockImageData);

        when(mockModels.generateImages(anyString(), anyString(), any(GenerateImagesConfig.class)))
                .thenReturn(imagesResponse);

        // Act
        ImageGenerationService.GeneratedImageData result =
                imageGenerationService.generateImage(VALID_PROMPT, null, style, USER_ID);

        // Assert
        assertNotNull(result);
        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(mockModels).generateImages(eq("imagen-4.0-fast-generate-001"), promptCaptor.capture(), any());
        assertTrue(promptCaptor.getValue().contains("\"prompt\""));
    }

    @Test
    void generateImage_ErroNaAPI_DeveLancarExternalServiceException() throws Exception {
        // Arrange
        ImageStyle style = mock(ImageStyle.class);
        when(style.getTemplate()).thenReturn("${word}");
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(mockUser));

        GenerateContentResponse contentResponse = mock(GenerateContentResponse.class);
        when(contentResponse.text()).thenReturn("prompt");
        when(mockModels.generateContent(anyString(), anyString(), isNull()))
                .thenReturn(contentResponse);

        when(mockModels.generateImages(anyString(), anyString(), any(GenerateImagesConfig.class)))
                .thenThrow(new RuntimeException("API Error"));

        // Act & Assert
        assertThrows(ExternalServiceException.class,
                () -> imageGenerationService.generateImage(VALID_PROMPT, null, style, USER_ID));

        verify(userRepository, never()).save(any());
    }

    @Test
    void generateImageAsync_ComSucesso_DeveRetornarCompletableFuture() throws Exception {
        // Arrange
        ImageStyle style = mock(ImageStyle.class);
        ImageGenerationService mockService = mock(ImageGenerationService.class);
        byte[] mockImageData = new byte[]{1, 2, 3};
        ImageGenerationService.GeneratedImageData expectedResult =
                new ImageGenerationService.GeneratedImageData(mockImageData, "image/png");

        when(applicationContext.getBean(ImageGenerationService.class)).thenReturn(mockService);
        when(mockService.generateImage(VALID_PROMPT, null, style, USER_ID))
                .thenReturn(expectedResult);

        // Act
        CompletableFuture<ImageGenerationService.GeneratedImageData> future =
                imageGenerationService.generateImageAsync(VALID_PROMPT, null, style, USER_ID);

        // Assert
        assertNotNull(future);
        ImageGenerationService.GeneratedImageData result = future.get();
        assertArrayEquals(mockImageData, result.imageBytes());
        assertEquals("image/png", result.mimeType());
    }

    @Test
    void generateImageAsync_ComFalha_DeveRetornarFutureComErro() {
        // Arrange
        ImageStyle style = mock(ImageStyle.class);
        ImageGenerationService mockService = mock(ImageGenerationService.class);

        when(applicationContext.getBean(ImageGenerationService.class)).thenReturn(mockService);
        when(mockService.generateImage(VALID_PROMPT, null, style, USER_ID))
                .thenThrow(new RuntimeException("Erro na geração"));

        // Act
        CompletableFuture<ImageGenerationService.GeneratedImageData> future =
                imageGenerationService.generateImageAsync(VALID_PROMPT, null, style, USER_ID);

        // Assert
        assertNotNull(future);
        assertTrue(future.isCompletedExceptionally());
    }

    @Test
    void generateImage_DeveUsarConfiguracoesCorretas() throws Exception {
        // Arrange
        ImageStyle style = mock(ImageStyle.class);
        when(style.getTemplate()).thenReturn("${word}");
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(mockUser));

        GenerateContentResponse contentResponse = mock(GenerateContentResponse.class);
        when(contentResponse.text()).thenReturn("prompt");
        when(mockModels.generateContent(anyString(), anyString(), isNull()))
                .thenReturn(contentResponse);

        byte[] mockImageData = new byte[]{1};
        GenerateImagesResponse imagesResponse = createMockImageResponse(mockImageData);

        when(mockModels.generateImages(anyString(), anyString(), any(GenerateImagesConfig.class)))
                .thenReturn(imagesResponse);

        // Act
        imageGenerationService.generateImage(VALID_PROMPT, null, style, USER_ID);

        // Assert
        ArgumentCaptor<GenerateImagesConfig> configCaptor = ArgumentCaptor.forClass(GenerateImagesConfig.class);
        verify(mockModels).generateImages(eq("imagen-4.0-fast-generate-001"), anyString(), configCaptor.capture());

        assertNotNull(configCaptor.getValue());
    }

    @Test
    void generateImage_TemplateSubstituicao_DeveSubstituirVariaveis() throws Exception {
        // Arrange
        ImageStyle style = mock(ImageStyle.class);
        when(style.getTemplate()).thenReturn("Draw ${word} in ${language} style");
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(mockUser));

        GenerateContentResponse contentResponse = mock(GenerateContentResponse.class);
        when(contentResponse.text()).thenReturn("processed prompt");
        when(mockModels.generateContent(eq("gemini-2.5-flash"), anyString(), isNull()))
                .thenReturn(contentResponse);

        byte[] mockImageData = new byte[]{1, 2};
        GenerateImagesResponse imagesResponse = createMockImageResponse(mockImageData);

        when(mockModels.generateImages(anyString(), anyString(), any(GenerateImagesConfig.class)))
                .thenReturn(imagesResponse);

        // Act
        imageGenerationService.generateImage("cat", null, style, USER_ID);

        // Assert
        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(mockModels).generateContent(eq("gemini-2.5-flash"), promptCaptor.capture(), isNull());

        String capturedPrompt = promptCaptor.getValue();
        assertTrue(capturedPrompt.contains("cat"));
        assertTrue(capturedPrompt.contains("ingles"));
    }



    @Test
    void naoDeveGerarNada_PromptVazio () throws IOException {
        mockUser.setBalance(100);
        when(userRepository.findById(mockUser.getId())).thenReturn(Optional.of(mockUser));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> imageGenerationService.generateImage("", null, ImageStyle.ANIME_STYLE,mockUser.getId()));


        assertEquals("Não é possível gerar imagem com prompt vazio.", exception.getMessage());
        assertEquals(100, mockUser.getBalance());

    }



    private GenerateImagesResponse createMockImageResponse(byte[] imageData) {
        GenerateImagesResponse imagesResponse = mock(GenerateImagesResponse.class);
        GeneratedImage generatedImage = mock(GeneratedImage.class);

        // Usar doReturn/when para evitar problemas com cast
        Image imageMock = mock(Image.class);
        doReturn(Optional.of(imageData)).when(imageMock).imageBytes();
        doReturn(Optional.of(imageMock)).when(generatedImage).image();

        when(imagesResponse.generatedImages()).thenReturn(Optional.of(List.of(generatedImage)));

        return imagesResponse;
    }

}