package com.spaced_repetition_ai.service;


import com.mongodb.lang.Nullable;
import com.spaced_repetition_ai.entity.DeckEntity;
import com.spaced_repetition_ai.repository.DeckRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class DeckService {

    private final DeckRepository deckRepository;

    public DeckService(DeckRepository deckRepository) {
        this.deckRepository = deckRepository;
    }

    public void createDeck(String name, String description,
                           @Nullable String targetLanguage, @Nullable String sourceLanguage,
                           @Nullable String audioPrompt, @Nullable String imagePrompt,
                           @Nullable String textPrompt, @Nullable String audioPath,
                           @Nullable String imagePath, @Nullable Double easeFactor){


        String finalSourceLanguage = Optional.ofNullable(sourceLanguage).orElse("pt-BR");
        String finalTargetLanguage = Optional.ofNullable(targetLanguage).orElse("en-US");
        double finalEaseFactor = Optional.ofNullable(easeFactor).orElse(2.5);
        String finalAudioPath = Optional.ofNullable(audioPath).orElse("Storage/");
        String finalImagePath = Optional.ofNullable(imagePath).orElse("Storage/");


        String standardTextPrompt = """
                Você é um engenheiro linguístico que cria frases para estudo em Anki. Seu trabalho é gerar frases simples, naturais e com vocabulário comum para facilitar o aprendizado de idiomas.
                
                Dado o input de uma palavra em um idioma (ex: inglês, espanhol, francês), sua tarefa é:
                
                1. Criar uma frase curta e natural nesse idioma usando essa palavra, com no máximo 10 palavras.
                2. Utilizar palavras frequentes e de uso cotidiano nesse idioma.
                3. Traduzir essa frase para a lingua nativa que será passada, de forma clara e precisa.
                4. Retornar somente um objeto JSON no seguinte formato:
                
                {
                  "front": "frase no idioma original aqui",
                  "back": "tradução na lingua nativa que irei te passar aqui"
                }
                
                ⚠️ Responda apenas com o JSON. Não adicione explicações extras, títulos ou comentários fora do JSON.
                """;

        String standardImagePrompt = """
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
                """;


        String finalTextPrompt = Optional.ofNullable(textPrompt).orElse(standardTextPrompt);
        String finalImagePrompt = Optional.ofNullable(imagePrompt).orElse(standardImagePrompt);
        String finalAudioPrompt = Optional.ofNullable(audioPrompt).orElse("");


        DeckEntity deckEntity = new DeckEntity(
                null,
                name,
                description,
                finalTargetLanguage,
                finalSourceLanguage,
                finalAudioPrompt,
                finalImagePrompt,
                finalTextPrompt,
                finalAudioPath,
                finalImagePath,
                finalEaseFactor
        );

        deckRepository.save(deckEntity);
        System.out.println("Deck criado com sucesso!");
        System.out.println("Deck ID: " + deckEntity.getId());
        System.out.println("Deck Name: " + deckEntity.getName());
        System.out.println("Deck Description: " + deckEntity.getDescription());
        System.out.println("Deck Source Language: " + deckEntity.getSourceLanguage());
        System.out.println("Deck Target Language: " + deckEntity.getTargetLanguage());
        System.out.println("Deck Audio Path: " + deckEntity.getAudioPath());


    }




}
