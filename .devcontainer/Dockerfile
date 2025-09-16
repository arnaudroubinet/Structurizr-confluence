# Utilise l'image officielle Ubuntu 24.04 comme base
FROM mcr.microsoft.com/devcontainers/base:ubuntu-24.04

# Installe OpenJDK 21 et Maven
RUN apt-get update \
    && apt-get install -y openjdk-21-jdk maven \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

ENV JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
ENV PATH="$JAVA_HOME/bin:$PATH"
