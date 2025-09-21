#!/bin/bash

# Build native executable for Structurizr Confluence CLI
# This script builds a native Linux executable using Docker

echo "🔨 Building Structurizr Confluence CLI native executable..."
echo ""

# Check if Docker is available for container build
if ! command -v docker &> /dev/null; then
    echo "❌ Docker is required for native compilation but not found"
    echo "   Please install Docker or use 'mvn clean package' for JVM build"
    exit 1
fi

# Clean and build native executable
echo "📦 Building native executable (this may take 5-10 minutes)..."
mvn clean package -Pnative -DskipTests

if [ $? -eq 0 ]; then
    echo ""
    echo "✅ Native executable built successfully!"
    echo ""
    echo "📍 Location: target/*-runner"
    echo "🔧 Usage:    ./target/*-runner --help"
    echo ""
    echo "To make it executable anywhere:"
    echo "  sudo cp target/*-runner /usr/local/bin/structurizr-confluence"
    echo "  structurizr-confluence --help"
else
    echo ""
    echo "❌ Native build failed. Try JVM build instead:"
    echo "   mvn clean package"
    exit 1
fi