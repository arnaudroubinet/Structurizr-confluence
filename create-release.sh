#!/usr/bin/env bash
set -euo pipefail

# Script to create and push a new release tag for Structurizr Confluence
# This script creates tag v1.1.0 (minor version increment from v1.0.20)

echo "🚀 Creating new release: v1.1.0"
echo ""

# Verify we're in the correct directory
if [ ! -f "pom.xml" ]; then
    echo "❌ Error: pom.xml not found. Please run this script from the repository root."
    exit 1
fi

# Check if we have git
if ! command -v git &> /dev/null; then
    echo "❌ Error: git is not installed"
    exit 1
fi

# Fetch latest changes and tags
echo "📥 Fetching latest changes from GitHub..."
git fetch --all --tags

# Get the latest tag
LATEST_TAG=$(git tag -l 'v*.*.*' | sort -V | tail -n 1)
echo "📌 Latest tag: ${LATEST_TAG}"

# Define new version
NEW_VERSION="v1.1.0"
echo "🆕 New version: ${NEW_VERSION}"

# Check if tag already exists
if git rev-parse "$NEW_VERSION" >/dev/null 2>&1; then
    echo "❌ Error: Tag ${NEW_VERSION} already exists!"
    echo ""
    echo "To delete it and recreate:"
    echo "  git tag -d ${NEW_VERSION}"
    echo "  git push --delete origin ${NEW_VERSION}"
    exit 1
fi

# Get the target commit (latest on main)
echo ""
echo "🎯 Target commit: main (latest)"
TARGET_COMMIT="main"

# Confirm with user
echo ""
echo "📋 Release Summary:"
echo "  Previous version: ${LATEST_TAG}"
echo "  New version:      ${NEW_VERSION}"
echo "  Target commit:    ${TARGET_COMMIT}"
echo ""
read -p "Do you want to create and push this release? (y/N) " -n 1 -r
echo ""

if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo "❌ Release cancelled"
    exit 0
fi

# Create the annotated tag
echo ""
echo "🏷️  Creating annotated tag ${NEW_VERSION}..."
git tag -a "${NEW_VERSION}" "${TARGET_COMMIT}" -m "Release version 1.1.0

Changes since ${LATEST_TAG}:
- Fix diagram resolution for view keys containing dashes (#27)
"

echo "✅ Tag created locally"

# Push the tag to GitHub
echo ""
echo "⬆️  Pushing tag to GitHub..."
git push origin "${NEW_VERSION}"

echo ""
echo "✅ Release tag ${NEW_VERSION} has been successfully created and pushed!"
echo ""
echo "🔔 Next steps:"
echo "  1. The GitHub Actions workflow will automatically build and release"
echo "  2. Check progress at: https://github.com/arnaudroubinet/Structurizr-confluence/actions"
echo "  3. View release at: https://github.com/arnaudroubinet/Structurizr-confluence/releases/tag/${NEW_VERSION}"
echo "  4. Docker image will be available at: ghcr.io/arnaudroubinet/structurizr-confluence:1.1.0"
echo ""
