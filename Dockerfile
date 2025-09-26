# Multi-stage Dockerfile for Structurizr Confluence CLI (JVM mode)
# Build stage: compile with Maven (Java 21)
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /work

# Leverage build cache for dependencies
COPY pom.xml ./
RUN --mount=type=cache,target=/root/.m2 mvn -B -q -e -DskipTests package || true

# Add sources and perform full build
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -B -DskipTests package

# Runtime stage: minimal JRE 21
FROM eclipse-temurin:21-jre AS runtime
ENV APP_HOME=/opt/structurizr \
    LANG=C.UTF-8 \
    JAVA_OPTS=""
WORKDIR ${APP_HOME}

LABEL org.opencontainers.image.source="https://github.com/arnaudroubinet/Structurizr-confluence" \
      org.opencontainers.image.title="Structurizr Confluence CLI" \
      org.opencontainers.image.description="CLI that exports Structurizr workspaces to Confluence in ADF format" \
      org.opencontainers.image.licenses="Apache-2.0"

# Install only Node.js and npm
RUN apt-get update && apt-get install -y \
    nodejs \
    npm \
    # Additional dependencies for Puppeteer/Chrome
    wget \
    gnupg \
    ca-certificates \
    fonts-liberation \
    libasound2t64 \
    libatk-bridge2.0-0 \
    libdrm2 \
    libgtk-3-0 \
    libnspr4 \
    libnss3 \
    libxss1 \
    libxtst6 \
    xdg-utils \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/* \
    && npm install puppeteer \
    && npm cache clean --force \
    && rm -rf /root/.npm \
    && rm -rf /tmp/* \
    && apt-get clean \
    && apt-get autoclean \
    && apt-get autoremove -y \
    && rm -rf /var/lib/apt/lists/* \
    && rm -rf /var/cache/apt/archives/* \
    && rm -rf /tmp/* \
    && rm -rf /var/tmp/*


# Copy Quarkus fast-jar layout
COPY --from=build /work/target/quarkus-app/ ./quarkus-app/

# Default execution (Picocli CLI). Provide args at docker run time.
ENTRYPOINT ["java","-jar","/opt/structurizr/quarkus-app/quarkus-run.jar"]
# Optional default arguments
CMD ["--help"]
