# ==============================================================================
# Dockerfile for LostLink Spring Boot Backend (Render / Cloud Deployment)
# Multi-stage build using Java 17 Temurin
# ==============================================================================

# Stage 1: Build Application
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Cache Maven dependencies layer
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy backend source code
COPY src ./src

# Build executable Spring Boot JAR
RUN mvn clean package -DskipTests -B

# Stage 2: Secure Production Runtime
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Run as non-root user
RUN groupadd -r lostlink && useradd -r -g lostlink lostlink

# Copy executable jar from builder stage
COPY --from=build /app/target/*.jar app.jar

# Render assigns dynamic port via PORT environment variable
ENV PORT=8081
EXPOSE ${PORT}

USER lostlink

ENTRYPOINT ["sh", "-c", "java -Djava.security.egd=file:/dev/./urandom -jar app.jar"]
