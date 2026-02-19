# =============================================================================
# Multi-Stage Production Dockerfile
# =============================================================================
# Stage 1: Build the WAR file using Maven
# Stage 2: Deploy to a hardened Tomcat runtime as a non-root user
# =============================================================================

# ========================== STAGE 1: BUILD ==========================
FROM maven:3.9-eclipse-temurin-17 AS builder

LABEL maintainer="devsecops-pipeline"
LABEL stage="builder"

WORKDIR /build

# Copy POM first to leverage Docker layer caching for dependencies
COPY app/pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build the WAR
COPY app/src ./src
RUN mvn package -DskipTests -B && \
    mv target/*.war target/vprofile.war


# ========================= STAGE 2: RUNTIME =========================
FROM tomcat:10.1-jre17-temurin-jammy

LABEL maintainer="devsecops-pipeline"
LABEL description="vProfile Application — Production-ready container"

# Remove default Tomcat webapps (security hardening)
RUN rm -rf /usr/local/tomcat/webapps/* && \
    rm -rf /usr/local/tomcat/webapps.dist

# Create a non-root user (CIS Docker Benchmark 4.1)
RUN groupadd -r appgroup && \
    useradd -r -g appgroup -d /usr/local/tomcat -s /sbin/nologin appuser && \
    chown -R appuser:appgroup /usr/local/tomcat

# Copy the built WAR from the builder stage
COPY --from=builder --chown=appuser:appgroup /build/target/vprofile.war /usr/local/tomcat/webapps/ROOT.war

# Switch to non-root user
USER appuser

# Expose the application port
EXPOSE 8080

# Health check for container orchestration
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/health || exit 1

# Start Tomcat
CMD ["catalina.sh", "run"]
