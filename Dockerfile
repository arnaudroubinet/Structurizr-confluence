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

# Install Playwright browser dependencies
RUN apt-get update && apt-get install -y \
    # Playwright browser dependencies
    libnss3 \
    libnspr4 \
    libatk-bridge2.0-0 \
    libdrm2 \
    libxss1 \
    libgconf-2-4 \
    libxtst6 \
    libxrandr2 \
    libasound2 \
    libpangocairo-1.0-0 \
    libatk1.0-0 \
    libcairo-gobject2 \
    libgtk-3-0 \
    libgdk-pixbuf2.0-0 \
    libxcomposite1 \
    libxcursor1 \
    libxdamage1 \
    libxfixes3 \
    libxi6 \
    libxinerama1 \
    libxss1 \
    libxext6 \
    libx11-6 \
    fonts-liberation \
    libappindicator3-1 \
    xdg-utils \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/* \
    && rm -rf /tmp/* \
    && rm -rf /var/tmp/*

# Copy Quarkus fast-jar layout
COPY --from=build /work/target/quarkus-app/ ./quarkus-app/

# Default execution (Picocli CLI). Provide args at docker run time.
ENTRYPOINT ["java","-jar","/opt/structurizr/quarkus-app/quarkus-run.jar"]
# Optional default arguments
CMD ["--help"]
