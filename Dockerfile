# ==============================================================================
# Root Dockerfile for LostLink Spring Boot Backend (Render Monorepo Support)
# Multi-stage build using Java 17 Temurin
# ==============================================================================

# Stage 1: Build Application
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Cache Maven dependencies layer
COPY lostfound/pom.xml .
RUN mvn dependency:go-offline -B

# Copy backend source code
COPY lostfound/src ./src

# Build executable Spring Boot JAR
RUN mvn clean package -DskipTests -B

# Stage 2: Secure Production Runtime
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

RUN groupadd -r lostlink && useradd -r -g lostlink lostlink
RUN mkdir -p /app/uploads && chown -R lostlink:lostlink /app

COPY --from=build /app/target/*.jar app.jar

ENV PORT=8081
EXPOSE ${PORT}

USER lostlink

ENTRYPOINT ["sh", "-c", "java -Djava.security.egd=file:/dev/./urandom -jar app.jar"]
