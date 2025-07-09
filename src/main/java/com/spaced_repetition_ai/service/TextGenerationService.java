package com.spaced_repetition_ai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;
import com.spaced_repetition_ai.model.FlashCard;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
public class TextGenerationService {

    private final Client genaiClient;
    private final AudioGenerationService audioGenAiService;
    private final ImageGenerationService imageGenAiService;

    public TextGenerationService(Client genaiClient, AudioGenerationService audioGenAiService, ImageGenerationService imageGenAiService) {
        this.genaiClient = genaiClient;
        this.audioGenAiService = audioGenAiService;
        this.imageGenAiService = imageGenAiService;
    }

    public FlashCard generateTextFromJson(String userInput) {

        String fullPrompt;
        fullPrompt = """
                Você é um engenheiro linguístico que cria frases para estudo em Anki. Seu trabalho é gerar frases simples, naturais e com vocabulário comum para facilitar o aprendizado de idiomas.
                
                Dado o input de uma palavra em um idioma (ex: inglês, espanhol, francês), sua tarefa é:
                
                1. Criar uma frase curta e natural nesse idioma usando essa palavra, com no máximo 10 palavras.
                2. Utilizar palavras frequentes e de uso cotidiano nesse idioma.
                3. Traduzir essa frase para português brasileiro, de forma clara e precisa.
                4. Retornar somente um objeto JSON no seguinte formato:
                
                {
                  "front": "frase no idioma original aqui",
                  "back": "tradução em português aqui"
                }
                
                ⚠️ Responda apenas com o JSON. Não adicione explicações extras, títulos ou comentários fora do JSON.
                
                Comece agora com a palavra: "%s"
                """.formatted(userInput);
        System.out.println(fullPrompt);

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

            **Palavra-chave para Geração da Imagem:**
   
    """.formatted(userInput);

        GenerateContentResponse response = GenerateTextFromTextInput(fullPrompt);
        String generatedJsonString = response.text();
        System.out.println("JSON recebido do Gemini (bruto): " + generatedJsonString);

        if (generatedJsonString.startsWith("```json")) {
            generatedJsonString = generatedJsonString.substring("```json".length()).trim();
        }
        if (generatedJsonString.endsWith("```")) {
            generatedJsonString = generatedJsonString.substring(0, generatedJsonString.length() - "```".length()).trim();
        }

        System.out.println("JSON após remoção dos delimitadores: " + generatedJsonString);

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        try {
            FlashCard generatedText = objectMapper.readValue(generatedJsonString, FlashCard.class);

            String flashCard = "Front: " + generatedText.getFront() + "\nBack: " + generatedText.getBack();


            return generatedText;
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return null;
        }
    }



    public GenerateContentResponse GenerateTextFromTextInput(String prompt) {


        GenerateContentResponse response =
                genaiClient.models.generateContent(
                        "gemini-2.5-flash",
                        prompt,
                        null);

        System.out.println(response.text());
        return response;
    }
}
