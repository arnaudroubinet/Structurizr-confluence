# Release v1.1.0

## Summary
This document provides instructions to create a new minor version release (v1.1.0) of Structurizr Confluence Exporter.

## Current State
- **Latest Version**: v1.0.20
- **Latest Commit on main**: `659d4d9cf3f54c7391d344449e8257ca15be355b`
- **New Version**: v1.1.0 (minor increment)

## Release Instructions

### Option 1: Using Git Command Line

```bash
# Ensure you're on the latest main branch
git checkout main
git pull origin main

# Create an annotated tag for v1.1.0
git tag -a v1.1.0 -m "Release version 1.1.0"

# Push the tag to GitHub (this will trigger the release workflow)
git push origin v1.1.0
```

### Option 2: Using GitHub CLI

```bash
# Ensure you're on the latest main branch
git checkout main
git pull origin main

# Create and push tag using GitHub CLI
gh release create v1.1.0 \
  --title "Structurizr Confluence 1.1.0" \
  --notes "Release version 1.1.0 - Minor version increment" \
  --target main
```

### Option 3: Using GitHub Web Interface

1. Go to https://github.com/arnaudroubinet/Structurizr-confluence/releases/new
2. Click on "Choose a tag" dropdown
3. Type `v1.1.0` and select "Create new tag: v1.1.0 on publish"
4. Set the target to `main` branch
5. Set the release title to "Structurizr Confluence 1.1.0"
6. Add release notes describing changes since v1.0.20
7. Click "Publish release"

## What Happens After Tag Push

The `.github/workflows/release.yml` workflow will automatically:

1. ✅ Build the project with Maven
2. ✅ Create a GitHub release with the tag
3. ✅ Upload the compiled JAR file as a release asset
4. ✅ Build and push a Docker image to GitHub Container Registry (GHCR)
   - Image tags: `ghcr.io/arnaudroubinet/structurizr-confluence:1.1.0` and `latest`

## Verification Steps

After creating the release, verify:

1. Tag appears in https://github.com/arnaudroubinet/Structurizr-confluence/tags
2. Release appears in https://github.com/arnaudroubinet/Structurizr-confluence/releases
3. Release workflow runs successfully in https://github.com/arnaudroubinet/Structurizr-confluence/actions
4. Docker image is available at `ghcr.io/arnaudroubinet/structurizr-confluence:1.1.0`

## Rollback (if needed)

If something goes wrong with the release:

```bash
# Delete the tag locally
git tag -d v1.1.0

# Delete the tag from GitHub
git push --delete origin v1.1.0

# Delete the release from GitHub (via web interface or CLI)
gh release delete v1.1.0 --yes
```

Then fix any issues and repeat the release process.
