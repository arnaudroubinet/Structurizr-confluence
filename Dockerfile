# Multi-stage Dockerfile for Structurizr Confluence CLI (JVM mode)
# Single-stage Dockerfile for Structurizr Confluence CLI (pre-built artifacts)
# Using runtime JRE 17 with all required dependencies
FROM eclipse-temurin:17-jre
ENV APP_HOME=/opt/structurizr \
    LANG=C.UTF-8 \
    JAVA_OPTS=""
WORKDIR ${APP_HOME}

LABEL org.opencontainers.image.source="https://github.com/arnaudroubinet/Structurizr-confluence" \
      org.opencontainers.image.title="Structurizr Confluence CLI" \
      org.opencontainers.image.description="CLI that exports Structurizr workspaces to Confluence in ADF format" \
      org.opencontainers.image.licenses="Apache-2.0"

# Install Playwright browser dependencies and additional required libraries
# Following Docker best practices: single RUN command, --no-install-recommends, cleanup
RUN apt-get update && apt-get install -y --no-install-recommends \
    # Core Playwright browser dependencies
    libnss3 \
    libnspr4 \
    libatk-bridge2.0-0 \
    libdrm2 \
    libxss1 \
    libxtst6 \
    libxrandr2 \
    libasound2t64 \
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
    libxext6 \
    libx11-6 \
    fonts-liberation \
    libappindicator3-1 \
    xdg-utils \
    # Additional required libraries for enhanced Playwright functionality
    libgstreamer1.0-0 \
    libatomic1 \
    libxslt1.1 \
    libwoff1 \
    libvpx9 \
    libevent-2.1-7t64 \
    libopus0 \
    libgstreamer-plugins-base1.0-0 \
    libgstreamer-gl1.0-0 \
    libgstreamer-plugins-bad1.0-0 \
    libwebpdemux2 \
    libharfbuzz-icu0 \
    libenchant-2-2 \
    libsecret-1-0 \
    libhyphen0 \
    libmanette-0.2-0 \
    libflite1 \
    gstreamer1.0-libav \
    # Clean up to minimize image size (Docker best practices)
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/* \
    && rm -rf /tmp/* \
    && rm -rf /var/tmp/*

# Copy pre-built Quarkus application (build using: mvn clean package)
# Use build context: docker build --build-arg BUILD_DIR=target .
ARG BUILD_DIR=target
COPY ${BUILD_DIR}/quarkus-app/ ./quarkus-app/

# Default execution (Picocli CLI). Provide args at docker run time.
ENTRYPOINT ["java","-jar","/opt/structurizr/quarkus-app/quarkus-run.jar"]
# Optional default arguments
CMD ["--help"]
