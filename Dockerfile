# Estágio 1: Build da Aplicação com Maven
# Usamos uma imagem que já tem o Maven e o JDK 17 para compilar o projeto.
FROM maven:3.9.6-eclipse-temurin-17 AS build

# Define o diretório de trabalho dentro do container.
WORKDIR /app

# Copia o arquivo pom.xml para o container para baixar as dependências.
COPY pom.xml .

# Baixa todas as dependências do projeto.
# Isso aproveita o cache de layers do Docker, para não ter de baixar tudo a cada alteração no código.
RUN mvn dependency:go-offline

# Copia todo o código-fonte do projeto para o container.
COPY src ./src

# Executa o build do projeto, gerando o arquivo .jar.
# O -DskipTests acelera o build por não executar os testes.
RUN mvn package -DskipTests

# Estágio 2: Execução da Aplicação
# Usamos uma imagem mais leve, apenas com o ambiente de execução Java (JRE), para rodar a aplicação.
FROM eclipse-temurin:17-jre-jammy

# Define o diretório de trabalho.
WORKDIR /app

# Copia o arquivo .jar gerado no estágio de build para a imagem final.
# O nome do .jar é definido pelo <artifactId> e <version> no seu pom.xml.
COPY --from=build /app/target/spaced-repetition-ai-0.0.1-SNAPSHOT.jar app.jar

# Expõe a porta 9090, que é a porta padrão do Spring Boot.
EXPOSE 9090

# Comando para iniciar a aplicação quando o container for executado.
ENTRYPOINT ["java", "-jar", "app.jar"]