# ==============================================================================
# Estagio 1: Build da aplicacao Java com Maven
# ==============================================================================
FROM maven:3.9.9-eclipse-temurin-21-alpine AS builder

WORKDIR /build

# Copia o pom.xml primeiro para aproveitar cache de dependencias
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copia o codigo-fonte e empacota o JAR
COPY src ./src
RUN mvn clean package -DskipTests -B


# ==============================================================================
# Estagio 2: Imagem final enxuta e endurecida (Hardened Runtime)
# ==============================================================================
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Atualiza pacotes base do Alpine para corrigir eventuais CVEs recentes da imagem
RUN apk update && apk upgrade --no-cache && \
    addgroup -S appgroup && adduser -S appuser -G appgroup

# Copia apenas o artefato compilado do estagio anterior com permissao para o usuario nao-root
COPY --from=builder --chown=appuser:appgroup /build/target/*.jar /app/app.jar

# Requisito DevSecOps: Nao executar o container como root
USER appuser

EXPOSE 8080

# Healthcheck nativo para verificar se a API esta respondendo
HEALTHCHECK --interval=30s --timeout=5s --start-period=20s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/api/health || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
