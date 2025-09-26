# Estágio 1: Build da Aplicação com Maven
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn package -DskipTests

# ---

# Estágio 2: Execução da Aplicação
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# MELHORIA: Usa um curinga (*) para copiar o .jar, não importa a versão.
COPY --from=build /app/target/*.jar app.jar

# A porta 9090 está correta, baseado no seu código.
EXPOSE 9090

ENTRYPOINT ["java", "-jar", "app.jar"]