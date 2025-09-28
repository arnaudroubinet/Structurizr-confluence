#!/usr/bin/env bash
set -euo pipefail

echo "🔨 Building Structurizr Confluence Docker Image..."

# Build the application first
echo "📦 Building Java application with Maven..."
mvn --no-transfer-progress clean install

# Build Docker image
echo "🐳 Building Docker image..."
docker build -t structurizr-confluence:latest .

# Show final image size
echo "📊 Final image information:"
docker images structurizr-confluence:latest

# Test the image
echo "🧪 Testing Docker image..."
docker run --rm structurizr-confluence:latest --version

echo "✅ Build completed successfully!"
echo ""
echo "Usage examples:"
echo "  docker run --rm structurizr-confluence:latest --help"
echo "  docker run --rm -e CONFLUENCE_URL=... -e CONFLUENCE_USER=... structurizr-confluence:latest export ..."