package com.spaced_repetition_ai.service;


import com.mongodb.lang.Nullable;
import com.spaced_repetition_ai.entity.DeckEntity;
import com.spaced_repetition_ai.model.DeckType;
import com.spaced_repetition_ai.model.Language;
import com.spaced_repetition_ai.repository.DeckRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;


@Service
public class DeckService {

    private final DeckRepository deckRepository;

    public DeckService(DeckRepository deckRepository) {
        this.deckRepository = deckRepository;
    }

    public List<DeckEntity> getDecks() {
        List<DeckEntity> decks = deckRepository.findAll();
        System.out.println("Encontrados " + decks.size() + " decks.");
        decks.forEach(deck -> System.out.println(deck.getName()));
        return decks;
    }


    public void updateDeck(String id, String description, String name, Language TargetLanguage, Language SourceLanguage, String AudioPrompt, String ImagePrompt, String TextPrompt, String AudioPath, String ImagePath, Double EaseFactor) {


        deckRepository.findById(id).map(ent -> {
                    ent.setDescription(description);
                    ent.setName(name);
                    ent.setTargetLanguage(TargetLanguage);
                    ent.setSourceLanguage(SourceLanguage);
                    ent.setAudioPrompt(AudioPrompt);
                    ent.setImagePrompt(ImagePrompt);
                    ent.setTextPrompt(TextPrompt);
                    ent.setAudioPath(AudioPath);
                    ent.setImagePath(ImagePath);
                    ent.setEaseFactor(EaseFactor);
            deckRepository.save(ent);
            System.out.println("Deck atualizado com sucesso!");
            return ent;
        });

    }


    public void removerDeck(String deckId) {
    deckRepository.deleteById(deckId);
    }


    public void createDeck(String name, String description,
                           @Nullable Language targetLanguage, @Nullable Language sourceLanguage,
                           @Nullable String audioPrompt, @Nullable String imagePrompt,
                           @Nullable String textPrompt, @Nullable String audioPath,
                           @Nullable String imagePath, @Nullable Double easeFactor,
                           @Nullable boolean generateAudio, @Nullable boolean generateImage,
                           @Nullable DeckType deckType
        ){


        Language finalSourceLanguage = Optional.ofNullable(sourceLanguage).orElse(Language.PORTUGUES_BRASIL);
        Language finalTargetLanguage = Optional.ofNullable(targetLanguage).orElse(Language.INGLES_EUA);
        double finalEaseFactor = Optional.ofNullable(easeFactor).orElse(2.5);
        String finalAudioPath = Optional.ofNullable(audioPath).orElse("Storage/");
        String finalImagePath = Optional.ofNullable(imagePath).orElse("Storage/");


        String standardTextPrompt = """
                🧠 Você é um modelo de linguagem configurado para operar em modo restrito. Seu comportamento é rigidamente controlado para retornar **apenas dados no formato JSON com formatação Markdown**, conforme especificado abaixo. Você **não tem liberdade para improvisar ou adicionar conteúdo fora dessas regras**.
                
                Sua principal função é **receber um prompt e um input fornecidos pelo usuário** (ex: uma instrução e uma palavra ou expressão) e gerar uma resposta dentro da estrutura exigida.
                
                ---
                
                📥 A SEGUIR, VOCÊ RECEBERÁ:
                1. Um **prompt personalizado do usuário**, com instruções específicas sobre o que fazer com o input.
                2. Um **input do usuário**, que será a palavra, expressão ou comando alvo da tarefa.
                
                ⚠️ IMPORTANTE: Sua resposta **deve seguir exclusivamente o formato abaixo**. Qualquer conteúdo fora desse padrão será rejeitado automaticamente pelo sistema consumidor da sua resposta.
                ⚠️ Responda apenas com o JSON. Não adicione explicações extras, títulos ou comentários fora do JSON.
                ---
                
                🔐 FORMATO OBRIGATÓRIO DE SAÍDA:
                
                Você **deve retornar exatamente** um único objeto JSON com os seguintes campos:
                
                ```json
                {
                  "front": "Texto principal ou conteúdo original aqui",
                  "back": "Texto complementar aqui contendo ** Palavra ou Frase ** em negrito Markdown"
                }
                """;


        String languageTextPrompt = """
                Você é um engenheiro linguístico especializado em ensino de idiomas. Sua tarefa é ajudar o usuário a aprender novas palavras ou expressões de forma natural, usando frases curtas e didáticas.
                
                Ao receber uma **palavra ou expressão em um idioma estrangeiro**, você deve:
                
                1. Criar uma **frase curta, simples e natural** nesse idioma usando essa palavra ou expressão. A frase deve ter **no máximo 10 palavras**.
                2. A frase deve usar **vocabulário frequente e cotidiano**, que seja fácil de entender e memorizar.
                3. Evite construções gramaticais incomuns, palavras raras ou jargões técnicos.
                4. A frase deve parecer algo que um falante nativo realmente diria no dia a dia.
                5. Em seguida, forneça a **tradução da frase** para a **língua nativa especificada pelo usuário** de forma clara, natural e com equivalência direta de sentido.
                6. Utilize markdown para destacar a palavra/frase enviada, utilize ** Palavra ou Frase ** para destacar a palavra/frase enviada.
                
                A saída será processada por um sistema externo que cuidará da estrutura de apresentação, portanto **não inclua formatação nem explicações adicionais**.
                
                ---
                
                🧠 Exemplo de entrada:
                - Palavra: *"dog"* 
                - Idioma da palavra: inglês 
                - Língua nativa do usuário: português
                
                ✅ Comportamento esperado:
                - Frase gerada: *"The **dog** is sleeping on the bed."*
                - Tradução: *"O **cachorro** está dormindo na cama."*
                
                ---
                
                💬 A partir de agora, aguarde a palavra ou expressão a ser processada.
                
                """;

        String languageImagePrompt = """
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


        String finalTextPrompt = Optional.ofNullable(textPrompt).orElse(languageTextPrompt);
        String finalImagePrompt = Optional.ofNullable(imagePrompt).orElse(languageImagePrompt);
        String finalAudioPrompt = Optional.ofNullable(audioPrompt).orElse("");
        boolean finalGenerateAudio = Optional.ofNullable(generateAudio).orElse(true);
        boolean finalGenerateImage = Optional.ofNullable(generateImage).orElse(true);
        DeckType finalDeckType = Optional.ofNullable(deckType).orElse(DeckType.LANGUAGE);

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
                finalEaseFactor,
                standardTextPrompt,
                finalGenerateImage,
                finalGenerateAudio,
                finalDeckType
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
