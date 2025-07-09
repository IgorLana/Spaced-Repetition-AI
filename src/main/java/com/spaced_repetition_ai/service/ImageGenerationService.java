package com.spaced_repetition_ai.service;

import com.google.common.collect.ImmutableList;
import com.google.genai.Client;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Part;
import com.spaced_repetition_ai.storage.ImageStorage;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

@Service
public class ImageGenerationService {


    private final String imageGenerationModel = "gemini-2.0-flash-preview-image-generation";

    private final Client genaiClient;
    private final ImageStorage imageStorage;

    public ImageGenerationService(Client genaiClient, ImageStorage imageStorage) {
        this.genaiClient = genaiClient;
        this.imageStorage = imageStorage;
    }



    public List<String> generateImage(String prompt, @Nullable List<MultipartFile> images){
        String imageprompt = """
            **Prompt para Geração de Imagens Educativas para Anki (720p)**

            **Objetivo:** Gerar uma imagem visualmente impactante e mnemônica para auxiliar na memorização de uma palavra-chave específica, otimizada para uso em flashcards digitais (Anki).

            ---

            **Requisitos da Imagem:**

            1.  **Resolução e Formato:**
                * Resolução exata: 720p (1280 pixels de largura por 720 pixels de altura).
                * Formato de arquivo: Preferencialmente PNG ou JPEG de alta qualidade para clareza e compatibilidade.

            2.  **Conteúdo Mnemônico:**
                * A imagem deve servir como um forte gatilho visual ou conceitual para a palavra-chave.
                * Não é obrigatório que a palavra-chave esteja presente na imagem em formato de texto. O foco é na associação visual.
                * Deve evocar uma conexão lógica, emocional ou simbólica que facilite a recordação da palavra.

            3.  **Composição e Foco:**
                * Composição limpa e desobstruída, com um ponto focal claro.
                * Minimizar elementos distrativos no fundo ou na cena para garantir que a atenção do usuário seja direcionada ao conceito principal.
                * A imagem deve ser facilmente compreendida em um rápido olhar.

            4.  **Estilo Visual:**
                * Estilo claro, direto e universalmente compreensível.
                * Evitar ambiguidades culturais, jargões ou referências muito específicas que possam não ser amplamente reconhecidas.
                * Pode ser ilustrativo, fotográfico ou abstrato, desde que atenda ao objetivo mnemônico.

            5.  **Qualidade Visual:**
                * **Paleta de Cores:** Harmoniosa, com bom contraste para garantir clareza e legibilidade em diferentes condições de tela (monitores, tablets, smartphones).
                * **Iluminação:** Consistente e bem distribuída, realçando os elementos principais da imagem e evitando sombras duras ou áreas subexpostas/superexpostas.
                * **Detalhes:** Níveis de detalhe apropriados para a resolução, sem sobrecarregar a imagem ou torná-la confusa.

            ---

            **Informações para Geração:**

            * **Contexto de Uso:** As imagens serão utilizadas em um ambiente de estudo repetitivo (Anki), onde a rapidez na associação e a memorização são cruciais.
            * **Função da Imagem:** A imagem atua como uma "pista" visual para a palavra-chave, ajudando o estudante a recuperar a informação da memória.
            * **Flexibilidade Criativa:** Embora os requisitos sejam específicos, há espaço para criatividade na forma como a associação visual é estabelecida, desde que o objetivo mnemônico seja atendido.

            ---

    **Palavra-chave para Geração da Imagem:** %s
   
    """.formatted(prompt);

        List<Part> parts = new ArrayList<>();
        parts.add(Part.fromText(imageprompt));

        if (images != null) {
            List<Part> imagePart = images.stream()
                    .map( image -> {
                        try {
                            return Part.fromBytes(image.getBytes(), image.getContentType());
                        } catch (IOException e) {
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .toList();
            parts.addAll(imagePart);
        }

        Content content = Content.builder().parts(parts).build();
        GenerateContentConfig config = GenerateContentConfig.builder()
                .responseModalities(List.of("Text", "Image"))
                .build();

        GenerateContentResponse response = this.genaiClient.models.generateContent(imageGenerationModel, content, config);
        List<Image> generateImage = getImages(response);

        List<String> savedImagePath = new ArrayList<>();
        for (Image image : generateImage) {
            String fullPath = imageStorage.saveImage(image.imageName(), image.imageBytes());
            savedImagePath.add(fullPath);
            System.out.println("Imagem salva: %s".formatted(fullPath));
        }
        return savedImagePath;
    }

    private List<Image> getImages(GenerateContentResponse response) {
        ImmutableList<Part> responseParts = response.parts();
        if (responseParts == null || responseParts.isEmpty()) {
            return Collections.emptyList();
        }
        return responseParts
                .stream()
                .map(Part::inlineData)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .filter(inlineData -> inlineData.data().isPresent())
                .map(inlineData -> {
                    MimeType mimeType = MimeType.valueOf(inlineData.mimeType().get()); // imageMimeType
                    return new Image(
                            "%s.%s".formatted(UUID.randomUUID().toString(), mimeType.getSubtype()),
                            inlineData.data().get(), // imageBytes
                            mimeType.toString());
                })
                .toList();
    }

    record Image(String imageName, byte[] imageBytes, String mimeType) {}

}
