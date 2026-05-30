# Multi-stage build for optimized Docker image
FROM eclipse-temurin:21-jdk AS builder

# Set working directory
WORKDIR /app

# Copy Gradle files
COPY gradle/ gradle/
COPY gradlew build.gradle.kts settings.gradle.kts ./

# Copy source code
COPY . .

# Build the application (skip tests to avoid running integration tests during image build)
RUN ./gradlew clean build shadowJar --no-daemon -x test

# Runtime stage
FROM eclipse-temurin:21-jre

# Install curl for health checks
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# Create non-root user
RUN groupadd -r appuser && useradd -r -g appuser appuser

# Set working directory
WORKDIR /app

# Copy the JAR file from builder stage
COPY --from=builder /app/build/libs/political-accountability-app-all.jar app.jar

# Copy runtime configuration files required by the application
RUN mkdir -p api-gateway/src/main/resources/logs
COPY api-gateway/src/main/resources/config.yml api-gateway/src/main/resources/config.yml

# Change ownership to non-root user
RUN chown appuser:appuser app.jar

# Ensure logs directory exists and is writable
RUN mkdir -p /app/logs && chown -R appuser:appuser /app/logs

# Switch to non-root user
USER appuser

# Expose ports
EXPOSE 8080 8081

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8081/healthcheck || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
