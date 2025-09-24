# Structurizr Confluence CLI

A high-performance command-line tool that exports [Structurizr](https://structurizr.com/) workspace documentation and Architecture Decision Records (ADRs) to Confluence Cloud.

## Features

- **⚡ Native Linux Executable** - Fast startup (~0.1s) with low memory footprint (~50MB)
- **🔗 Dual Workspace Sources** - Export from local JSON files or Structurizr on-premise
- **🎯 Targeted Operations** - Clean specific pages by title or ID, not entire spaces
- **🔒 Safety Features** - Interactive confirmation prompts with force override for automation
- **🌍 Environment Variables** - Comprehensive support for secure credential management
- **📄 Rich Documentation** - Exports workspace docs, model elements, views, and ADRs
- **🏗️ ADF Format** - Native Atlassian Document Format for perfect Confluence integration

## Quick Start

### Download & Install

```bash
# Download native executable (no Java required)
wget https://github.com/arnaudroubinet/Structurizr-confluence/releases/latest/download/structurizr-confluence-linux.tar.gz
tar -xzf structurizr-confluence-linux.tar.gz
chmod +x structurizr-confluence-linux

# Verify installation
./structurizr-confluence-linux --help
```

### Basic Usage

```bash
# Export workspace from local file
./structurizr-confluence-linux export \
  --confluence-url https://company.atlassian.net \
  --confluence-user user@company.com \
  --confluence-token api-token \
  --confluence-space MYSPACE \
  --workspace-file workspace.json

# Export from Structurizr on-premise
./structurizr-confluence-linux export \
  --structurizr-url https://structurizr.company.com \
  --structurizr-workspace-id 12345 \
  --structurizr-api-key key \
  --structurizr-api-secret secret \
  --confluence-url https://company.atlassian.net \
  --confluence-user user@company.com \
  --confluence-token api-token \
  --confluence-space MYSPACE
```

## Commands

### Export Workspace

Export documentation and ADRs from a Structurizr workspace to Confluence.

**Source Options (choose one):**
- `--workspace-file` - Export from local JSON file
- `--structurizr-*` options - Export from Structurizr on-premise

```bash
# Export from local file
./structurizr-confluence-linux export \
  --workspace-file workspace.json \
  --confluence-url https://company.atlassian.net \
  --confluence-user user@company.com \
  --confluence-token token \
  --confluence-space MYSPACE

# Export from Structurizr on-premise
./structurizr-confluence-linux export \
  --structurizr-url https://structurizr.company.com \
  --structurizr-workspace-id 12345 \
  --structurizr-api-key key \
  --structurizr-api-secret secret \
  --confluence-url https://company.atlassian.net \
  --confluence-user user@company.com \
  --confluence-token token \
  --confluence-space MYSPACE

# Clean existing pages before export
./structurizr-confluence-linux export \
  --workspace-file workspace.json \
  --confluence-space MYSPACE \
  --clean

# Target specific page for cleaning
./structurizr-confluence-linux export \
  --workspace-file workspace.json \
  --clean \
  --page-title "Architecture Documentation"

# Force operation (no confirmation)
./structurizr-confluence-linux export \
  --workspace-file workspace.json \
  --clean \
  --force
```

**Options:**
- `--workspace-file` - Path to workspace JSON file
- `--structurizr-url` - Structurizr on-premise URL  
- `--structurizr-workspace-id` - Workspace ID to load
- `--structurizr-api-key` - Structurizr API key
- `--structurizr-api-secret` - Structurizr API secret
- `--confluence-url` - Confluence base URL
- `--confluence-user` - User email
- `--confluence-token` - API token
- `--confluence-space` - Space key (required for page titles)
- `--branch` - Branch name for page naming (default: main)
- `--clean` - Clean target pages before export
- `--page-title` - Target specific page by title
- `--page-id` - Target specific page by ID
- `--force` - Skip confirmation prompts

### Clean Pages

Remove a page and all its subpages from Confluence.

```bash
# Clean by page title (requires space)
./structurizr-confluence-linux clean \
  --confluence-url https://company.atlassian.net \
  --confluence-user user@company.com \
  --confluence-token token \
  --confluence-space MYSPACE \
  --page-title "Old Documentation"

# Clean by page ID (globally unique)
./structurizr-confluence-linux clean \
  --confluence-url https://company.atlassian.net \
  --confluence-user user@company.com \
  --confluence-token token \
  --page-id "123456"

# Force deletion without confirmation
./structurizr-confluence-linux clean \
  --page-title "Old Documentation" \
  --confluence-space MYSPACE \
  --force
```

⚠️ **Warning**: Deletes the target page and ALL subpages. Prompts for confirmation unless `--force` is used.

### Load from Structurizr

Load workspace from Structurizr on-premise and export to Confluence in one step.

```bash
./structurizr-confluence-linux load \
  --structurizr-url https://structurizr.company.com \
  --structurizr-key key \
  --structurizr-secret secret \
  --workspace-id 12345 \
  --confluence-url https://company.atlassian.net \
  --confluence-user user@company.com \
  --confluence-token token \
  --confluence-space MYSPACE
java -jar structurizr-confluence-1.0.0.jar load \
  --structurizr-url https://structurizr.yourcompany.com \
  --structurizr-key your-api-key \
  --structurizr-secret your-api-secret \
  --workspace-id 12345 \
  --confluence-url https://yourcompany.atlassian.net \
  --confluence-user your-email@company.com \
  --confluence-token your-api-token \
  --confluence-space SPACE \
  --clean
```

### Environment Variables

You can use environment variables instead of command-line options for all CLI commands. The CLI will automatically detect and use these variables, logging which ones are being used (without showing the actual values for security):

**Confluence Variables:**
```bash
export CONFLUENCE_URL="https://yourcompany.atlassian.net"
export CONFLUENCE_USER="your-email@company.com"
export CONFLUENCE_TOKEN="your-api-token"
export CONFLUENCE_SPACE_KEY="SPACE"
```

**Structurizr Variables (for export from on-premise):**
```bash
export STRUCTURIZR_URL="https://structurizr.company.com"
export STRUCTURIZR_API_KEY="your-api-key"
export STRUCTURIZR_API_SECRET="your-api-secret"
export STRUCTURIZR_WORKSPACE_ID="12345"
```

**Example Usage with Environment Variables:**
```bash
# Set all variables
export CONFLUENCE_URL="https://yourcompany.atlassian.net"
export CONFLUENCE_USER="your-email@company.com"
export CONFLUENCE_TOKEN="your-api-token"
export CONFLUENCE_SPACE_KEY="SPACE"

```

## Environment Variables

The CLI supports environment variables for all configuration options. This is ideal for CI/CD and secure deployments.

**Confluence Settings:**
- `CONFLUENCE_URL` - Confluence base URL
- `CONFLUENCE_USER` - User email
- `CONFLUENCE_TOKEN` - API token
- `CONFLUENCE_SPACE_KEY` - Space key

**Structurizr Settings:**
- `STRUCTURIZR_URL` - Structurizr on-premise URL
- `STRUCTURIZR_API_KEY` - API key
- `STRUCTURIZR_API_SECRET` - API secret
- `STRUCTURIZR_WORKSPACE_ID` - Workspace ID

**Usage Example:**
```bash
# Set environment variables
export CONFLUENCE_URL="https://company.atlassian.net"
export CONFLUENCE_USER="user@company.com"
export CONFLUENCE_TOKEN="token"
export CONFLUENCE_SPACE_KEY="MYSPACE"

# Minimal commands using environment variables
./structurizr-confluence-linux export --workspace-file workspace.json
./structurizr-confluence-linux clean --page-title "Old Docs"
```

The CLI shows which environment variables are used without exposing sensitive values:
```
INFO Using CONFLUENCE_URL environment variable
INFO Using CONFLUENCE_TOKEN environment variable
```

## Docker: utiliser le CLI + Chrome

Une image Docker contenant le CLI natif, Chrome/Playwright et Graphviz est fournie et publiée sur GHCR.

- Image: `ghcr.io/arnaudroubinet/structurizr-confluence:latest`
- Contient:
  - Binaire natif `structurizr-confluence`
  - Node + Playwright (Chrome) + Graphviz
  - Scripts utilitaires: `export-diagrams.js`, `export-diagrams.playwright.js`

Exemples d’utilisation:

- Afficher l’aide:

```bash
docker run --rm ghcr.io/arnaudroubinet/structurizr-confluence:latest --help
```

- Exporter vers Confluence (variables d’env nécessaires):

```bash
docker run --rm \
  -e CONFLUENCE_URL="https://your-domain.atlassian.net" \
  -e CONFLUENCE_USER="email@example.com" \
  -e CONFLUENCE_TOKEN="<api-token>" \
  -v "$PWD":/work \
  ghcr.io/arnaudroubinet/structurizr-confluence:latest \
  --workspace /work/demo/itms-workspace.json \
  --space "Test" \
  --page-id 10977556
```

- Exécuter les scripts d’export de diagrammes basés sur Playwright/Puppeteer:

```bash
# Playwright (Chrome déjà présent dans l’image)
docker run --rm -v "$PWD":/work -w /opt/cli ghcr.io/arnaudroubinet/structurizr-confluence:latest \
  node export-diagrams.playwright.js

# Puppeteer (Installe automatiquement Chromium si besoin)
docker run --rm -v "$PWD":/work -w /opt/cli ghcr.io/arnaudroubinet/structurizr-confluence:latest \
  node export-diagrams.js
```

## Building from Source

### Prerequisites
- Java 21+
- Maven 3.6+
- Docker (for native compilation)

### Build Options

```bash
# Clone repository
git clone https://github.com/arnaudroubinet/Structurizr-confluence.git
cd Structurizr-confluence

# Build JAR
mvn clean package

# Build native executable (Linux)
mvn clean package -Pnative

# Run tests
mvn test
```

### Dev Container Setup

If you open this repository in VS Code Dev Containers, dependencies required for headless Chromium/Puppeteer and GraphViz are installed automatically via `postCreateCommand`:

- Installs Ubuntu packages for Chromium and GraphViz
- Runs `npm ci` and ensures Puppeteer browser binaries are available

Manual run (outside of Dev Container):

```
bash scripts/init-dev.sh
```

**Native Executable Benefits:**
- ⚡ Fast startup (~0.1s vs ~2s JVM)
- 💾 Low memory (~50MB vs ~200MB JVM)
- 📦 Single binary, no Java required
- 🐧 Perfect for containers and CI/CD

## What Gets Exported

The tool exports comprehensive documentation from your Structurizr workspace:

**Documentation Pages:**
- Workspace overview and description
- Software architecture documentation
- All embedded documentation sections

**Model Elements:**
- People (users, stakeholders)
- Software Systems (internal/external)
- Containers (applications, services, databases)
- Components (modules, classes, interfaces)

**Views & Diagrams:**
- System Landscape views
- System Context views  
- Container views
- Component views
- Deployment views
- Custom views

**Architecture Decision Records (ADRs):**
- Decision status and dates
- Context and problems
- Considered options
- Consequences and trade-offs

All content is converted to native Atlassian Document Format (ADF) for perfect Confluence integration with proper formatting, links, and structure.

## License

This project is licensed under the Apache License 2.0.

## Contributing

Contributions are welcome! Please ensure all tests pass before submitting pull requests.
