# =============================================================================
# Multi-Stage Production Dockerfile — CloudPulse API
# =============================================================================
# Stage 1: Build the application JAR using Maven
# Stage 2: Deploy to a minimal JRE runtime as a non-root user
# =============================================================================

# ========================== STAGE 1: BUILD ==========================
FROM maven:3.9-eclipse-temurin-17 AS builder

LABEL maintainer="devsecops-pipeline"
LABEL stage="builder"

WORKDIR /build

# Copy POM first to leverage Docker layer caching for dependency downloads
COPY app/pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build the JAR
COPY app/src ./src
RUN mvn package -DskipTests -B


# ========================= STAGE 2: RUNTIME =========================
FROM eclipse-temurin:17-jre-jammy

LABEL maintainer="devsecops-pipeline"
LABEL description="CloudPulse — Cloud Infrastructure Monitoring API"

# Install curl for health checks
RUN apt-get update && apt-get install -y --no-install-recommends curl && \
    rm -rf /var/lib/apt/lists/*

# Create a non-root user (CIS Docker Benchmark 4.1)
RUN groupadd -r appgroup && \
    useradd -r -g appgroup -d /app -s /sbin/nologin appuser

WORKDIR /app

# Copy the built JAR from the builder stage
COPY --from=builder --chown=appuser:appgroup /build/target/cloudpulse.jar app.jar

# Switch to non-root user
USER appuser

# Expose the application port
EXPOSE 8080

# JVM tuning for containers
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UseG1GC"

# Health check for container orchestration
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/api/actuator/health || exit 1

# Start the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
