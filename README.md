# Spaced Repetition AI

Spaced Repetition AI é uma aplicação backend robusta para um sistema de aprendizado por repetição espaçada. A plataforma utiliza o poder da Inteligência Artificial do Google Gemini para gerar flashcards de forma automática e dinâmica, incluindo texto, imagens e áudio, tornando o processo de estudo mais eficiente e engajante.

O sistema resolve o problema da criação manual e demorada de conteúdo de estudo, permitindo que os usuários gerem flashcards ricos e multimodais a partir de um simples comando ou palavra.

<br>

<div align="center" style="display: flex; flex-wrap: wrap; justify-content: center; align-items: center; gap: 10px;">
  <img src="https://cdn.jsdelivr.net/gh/devicons/devicon@latest/icons/java/java-original-wordmark.svg" width="70" />
  <img src="https://cdn.jsdelivr.net/gh/devicons/devicon@latest/icons/spring/spring-original-wordmark.svg" width="70" />
  <img src="https://cdn.jsdelivr.net/gh/devicons/devicon@latest/icons/postgresql/postgresql-plain-wordmark.svg" width="70" />
  <img src="https://cdn.jsdelivr.net/gh/devicons/devicon@latest/icons/maven/maven-original.svg" width="70" />
  <img src="https://cdn.jsdelivr.net/gh/devicons/devicon@latest/icons/junit/junit-original-wordmark.svg" width="70" />
          
</div>
---

## 📋 Tabela de Conteúdos

1.  [Funcionalidades](#-funcionalidades)
2.  [Tecnologias Utilizadas](#-tecnologias-utilizadas)
3.  [Instalação e Configuração](#️-instalação-e-configuração)
4.  [Guia da API (Endpoints)](#-guia-da-api-endpoints)
5.  [Arquitetura do Projeto](#️-arquitetura--estrutura-do-projeto)
6.  [Testes](#-testes)
7.  [Autor](#-autor)

---

## ✨ Funcionalidades

* **Autenticação Segura:** Sistema de registro e login de usuários com autenticação baseada em JSON Web Tokens (JWT).
* **Gerenciamento de Decks:** Criação, leitura, atualização e exclusão (CRUD) de decks de estudo personalizados.
* **Gerenciamento de Flashcards:** CRUD completo para flashcards dentro dos decks.
* **Geração de Conteúdo com IA:**
    * **Texto:** Criação automática do conteúdo do flashcard (frente e verso) com base em um prompt.
    * **Imagens:** Geração de imagens mnemônicas relacionadas ao conteúdo do flashcard.
    * **Áudio:** Geração de áudio (ex: pronúncia) para o conteúdo do flashcard.
* **Sistema de Repetição Espaçada:** Algoritmo que calcula o intervalo ideal para a próxima revisão de cada flashcard, otimizando a memorização.
* **Controle de Uso de IA:** Sistema de "saldo" por usuário para gerenciar e limitar o consumo dos recursos de IA.
* **Armazenamento de Mídia:** Serviço para armazenar e servir as imagens e áudios gerados.

---

## 🚀 Tecnologias Utilizadas

O projeto foi construído utilizando tecnologias modernas e robustas para garantir performance e escalabilidade.

* **Linguagem:** Java 17+
* **Framework:** Spring Boot 3
    * **Spring Web:** Para a construção de APIs RESTful.
    * **Spring Security:** Para a implementação da autenticação e autorização.
    * **Spring Data JPA:** Para a persistência de dados e comunicação com o banco.
* **Banco de Dados:** PostgreSQL
* **Autenticação:** JSON Web Tokens (JWT)
* **Inteligência Artificial:** Google Gemini API
* **Testes:**
    * JUnit 5
    * Mockito
    * H2 (Banco de dados em memória para testes)

---

## 🛠️ Instalação e Configuração

Siga os passos abaixo para configurar e executar o projeto em seu ambiente local.

**Pré-requisitos:**
* JDK 17 ou superior
* Maven 3.8 ou superior
* PostgreSQL instalado e em execução

**1. Clone o Repositório**
```bash
git clone [https://github.com/IgorLana/Spaced-Repetition-AI.git]
cd spaced-repetition-ai
```

**2. Configure o Banco de Dados**
Crie um banco de dados no PostgreSQL para a aplicação. Por exemplo: `spaced_repetition_db`.

**3. Configure as Variáveis de Ambiente**
Ajuste o arquivo `application.properties` com as suas credenciais e chaves.

`src/main/resources/application.properties`
```properties
# Configuração do Banco de Dados
spring.datasource.url=jdbc:postgresql://localhost:5432/SEU_BANCO_DE_DADOS
spring.datasource.username=SEU_USUARIO
spring.datasource.password=SUA_SENHA

# Chave secreta para JWT (gere uma chave segura e forte)
jwt.secret-key=SUA_CHAVE_SECRETA_PARA_JWT_COM_PELO_MENOS_256_BITS

# Chave da API do Google Gemini
gemini.api.key=SUA_API_KEY_DO_GEMINI

# Porta do servidor
server.port=9090
```

**4. Configure o Diretório de Armazenamento**
A aplicação salva as imagens e áudios gerados localmente. Certifique-se de que o caminho configurado no arquivo `StaticResourceConfig.java` exista ou ajuste-o conforme sua necessidade.

`src/main/java/com/spaced_repetition_ai/config/StaticResourceConfig.java`
```java
@Configuration
public class StaticResourceConfig implements WebMvcConfigurer {
    // Altere este caminho para um diretório válido em sua máquina
    private static final String STORAGE_PATH = "file:/caminho/para/sua/pasta/Storage/";

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry){
        registry.addResourceHandler("/storage/**")
                .addResourceLocations(STORAGE_PATH)
                .setCachePeriod(3600);
    }
}
```

**5. Execute a Aplicação**
Use o Maven para compilar e iniciar o servidor Spring Boot.
```bash
mvn spring-boot:run
```
O servidor estará disponível em `http://localhost:9090`.

---

## ⚡ Guia da API (Endpoints)

Todos os endpoints, exceto os de autenticação, são protegidos e requerem um token JWT válido no cabeçalho da requisição: `Authorization: Bearer <seu-token-jwt>`.

### 1. Autenticação (`/api/auth`)

Endpoints públicos para gerenciamento de contas de usuário.

* **`POST /api/auth/register`**
    * **Descrição:** Registra um novo usuário no sistema.
    * **Autenticação:** Nenhuma.
    * **Request Body:**
        ```json
        {
          "username": "novo_usuario",
          "email": "usuario@exemplo.com",
          "password": "senha_forte_123"
        }
        ```
    * **Resposta (Sucesso `200 OK`):** Retorna um token JWT para o usuário recém-criado.
        ```json
        {
          "token": "ey..."
        }
        ```

* **`POST /api/auth/login`**
    * **Descrição:** Autentica um usuário existente e retorna um token JWT.
    * **Autenticação:** Nenhuma.
    * **Request Body:**
        ```json
        {
          "username": "novo_usuario",
          "password": "senha_forte_123"
        }
        ```
    * **Resposta (Sucesso `200 OK`):** Retorna um token JWT válido.
        ```json
        {
          "token": "ey..."
        }
        ```

### 2. Decks (`/api/deck`)

Endpoints para gerenciar os decks de estudo do usuário autenticado.

* **`GET /api/deck`**
    * **Descrição:** Retorna uma lista de todos os decks pertencentes ao usuário.
    * **Resposta (Sucesso `200 OK`):** Um array de objetos `DeckResponseDTO`.

* **`POST /api/deck`**
    * **Descrição:** Cria um novo deck de flashcards.
    * **Request Body:** Um objeto `DeckRequestDTO`.
        ```json
        {
            "name": "Vocabulário de Espanhol",
            "description": "Palavras essenciais para iniciantes.",
            "targetLanguage": "ESPANHOL_EUA",
            "sourceLanguage": "PORTUGUES_BRASIL",
            "generateImage": true,
            "generateAudio": true,
            "deckType": "LANGUAGE"
        }
        ```

* **`PUT /api/deck/{id}`**
    * **Descrição:** Atualiza as informações de um deck existente.
    * **Parâmetro de URL:** `id` (o ID do deck a ser atualizado).
    * **Request Body:** Um objeto `DeckUpdateDTO` com os campos a serem modificados.

* **`DELETE /api/deck/{deckId}`**
    * **Descrição:** Exclui um deck e todos os flashcards associados a ele.
    * **Parâmetro de URL:** `deckId` (o ID do deck a ser excluído).

### 3. Flashcards (`/api/flashcard`)

Endpoints para gerenciar flashcards dentro de um deck.

* **`GET /api/flashcard`**
    * **Descrição:** Lista todos os flashcards de um deck específico.
    * **Parâmetro de Query:** `deckId` (o ID do deck).
    * **Resposta (Sucesso `200 OK`):** Um array de `FlashcardResponseDTO`. Retorna `204 No Content` se o deck estiver vazio.

* **`POST /api/flashcard`**
    * **Descrição:** Cria um novo flashcard manualmente.
    * **Parâmetro de Query:** `deckId` (o ID do deck onde o card será adicionado).
    * **Request Body:** Um objeto `FlashcardRequestDTO`.
        ```json
        {
          "front": "Hola",
          "back": "Olá"
        }
        ```

* **`POST /api/flashcard/ai`**
    * **Descrição:** Gera e salva um flashcard completo (texto, imagem, áudio) usando IA. O custo da operação é debitado do saldo do usuário.
    * **Parâmetros de Query:**
        * `prompt` (a palavra ou conceito para gerar o card, ex: "gato").
        * `deckId` (o ID do deck onde o card será adicionado).
    * **Resposta (Sucesso `200 OK`):** O `FlashcardResponseDTO` recém-criado.

* **`PUT /api/flashcard/{id}`**
    * **Descrição:** Atualiza o conteúdo de um flashcard existente.
    * **Parâmetro de URL:** `id` (o ID do flashcard).
    * **Request Body:** Um objeto `FlashcardRequestDTO` com os novos dados.

* **`DELETE /api/flashcard/{flashCardId}`**
    * **Descrição:** Exclui um flashcard específico.
    * **Parâmetro de URL:** `flashCardId` (o ID do flashcard a ser excluído).

### 4. Revisão (`/api/review`)

Endpoints para o sistema de repetição espaçada.

* **`GET /api/review`**
    * **Descrição:** Retorna a lista de flashcards que estão prontos para serem revisados em um deck específico.
    * **Parâmetro de Query:** `id` (o ID do deck).
    * **Resposta (Sucesso `200 OK`):** Um array de `FlashCardEntity`. Retorna `204 No Content` se não houver cards para revisar.

* **`POST /api/review`**
    * **Descrição:** Submete o resultado de uma revisão para um flashcard, atualizando sua próxima data de revisão.
    * **Parâmetros de Query:**
        * `id` (o ID do flashcard que foi revisado).
        * `review` (a avaliação do usuário). Valores possíveis: `ERRADO`, `DIFICIL`, `BOM`, `FACIL`.

---

## 🏛️ Arquitetura / Estrutura do Projeto

O projeto segue uma arquitetura em camadas, padrão em aplicações Spring Boot, para garantir a separação de responsabilidades e a manutenibilidade.

* `src/main/java/com/spaced_repetition_ai`
    * `config`: Classes de configuração do Spring (Segurança, Beans, CORS).
    * `controller`: Camada de API, responsável por expor os endpoints REST.
    * `dto`: Data Transfer Objects, para a comunicação entre cliente e servidor.
    * `entity`: Entidades JPA que mapeiam as tabelas do banco de dados.
    * `exception`: Classes de tratamento de exceções customizadas.
    * `model`: Enums e classes de modelo de negócio.
    * `repository`: Interfaces do Spring Data JPA para acesso ao banco de dados.
    * `service`: Camada de serviço, onde reside a lógica de negócio.
    * `storage`: Classes responsáveis pelo armazenamento de arquivos.
    * `util`: Classes utilitárias, como prompts padrão.

---

## 🧪 Testes

O projeto possui uma suíte de testes unitários para garantir a qualidade e o correto funcionamento da lógica de negócio.

Para executar os testes, utilize o seguinte comando Maven:
```bash
mvn test
```
Os testes são executados com um banco de dados H2 em memória, sem a necessidade de uma instância do PostgreSQL em execução.

---

## 👤 Autor

* **[Igor Lana]**
* **LinkedIn:** [(https://linkedin.com/in/igor-lana/)]
* **GitHub:** [(https://github.com/IgorLana/)]
* **Email:** [igorlana1@outlook.com]
