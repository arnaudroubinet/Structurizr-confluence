# Structurizr Confluence Exporter

A Java CLI application that exports Structurizr workspace documentation and Architecture Decision Records (ADRs) to Confluence Cloud in Atlassian Document Format (ADF).

## Overview

This application enables organizations to automatically synchronize their software architecture documentation from Structurizr workspaces into Confluence Cloud, making it accessible to broader teams through a familiar collaboration platform.

### Key Features

- ✅ Export Structurizr documentation and diagrams to Confluence
- ✅ Convert Architecture Decision Records (ADRs) to ADF format
- ✅ Automated diagram export with Playwright
- ✅ Branch-based documentation workflows
- ✅ Docker support for containerized execution
- ✅ GitHub Actions CI/CD integration

## Quick Start

### Prerequisites

- Java 21 or higher
- Maven 3.6+
- Confluence Cloud instance with API access
- Structurizr workspace

### Installation

#### Using Docker (Recommended)

```bash
docker pull ghcr.io/arnaudroubinet/structurizr-confluence:latest

docker run --rm \
  -e CONFLUENCE_URL=https://your-domain.atlassian.net \
  -e CONFLUENCE_USER=your-email@domain.com \
  -e CONFLUENCE_TOKEN=your-api-token \
  -e CONFLUENCE_SPACE_KEY=YOUR_SPACE \
  ghcr.io/arnaudroubinet/structurizr-confluence:latest export --workspace-id 123
```

#### Building from Source

```bash
# Clone the repository
git clone https://github.com/arnaudroubinet/Structurizr-confluence.git
cd Structurizr-confluence

# Build with Maven
mvn clean install

# Run the application
java -jar target/quarkus-app/quarkus-run.jar export --workspace-id 123
```

## Usage

### Basic Export

Export a Structurizr workspace to Confluence:

```bash
java -jar target/quarkus-app/quarkus-run.jar export \
  --workspace-id 123 \
  --confluence-url https://your-domain.atlassian.net \
  --confluence-user your-email@domain.com \
  --confluence-token your-api-token \
  --space-key YOUR_SPACE \
  --parent-page-title "Architecture Documentation"
```

### Branch-Based Export

Export to a branch-specific subpage:

```bash
java -jar target/quarkus-app/quarkus-run.jar export \
  --workspace-id 123 \
  --page-id 123456789 \
  --branch "feature/new-architecture"
```

### Environment Variables

You can use environment variables instead of command-line arguments:

```bash
export CONFLUENCE_URL=https://your-domain.atlassian.net
export CONFLUENCE_USER=your-email@domain.com
export CONFLUENCE_TOKEN=your-api-token
export CONFLUENCE_SPACE_KEY=YOUR_SPACE

java -jar target/quarkus-app/quarkus-run.jar export --workspace-id 123
```

## Documentation

- [Docker Usage Guide](DOCKER.md) - Comprehensive Docker build and usage instructions
- [Release Process](RELEASING.md) - How to create new releases
- [GitHub Copilot Instructions](.github/copilot-instructions.md) - Development guidelines for contributors

## GitHub Copilot Instructions

This repository includes intelligent GitHub Copilot instruction files to enhance your development experience.

### Available Instructions

The project includes curated instruction files from the [awesome-copilot](https://github.com/github/awesome-copilot) repository:

- ✅ **Java Development** - Java 21 best practices and patterns
- ✅ **Quarkus** - Framework-specific development standards
- ✅ **Docker Best Practices** - Container optimization and security

### Discovering More Instructions

Use our built-in tools to discover and install additional relevant instructions:

```bash
# See suggestions based on your project context
python .github/agents/suggest-instructions.py

# Download recommended instructions
python .github/agents/download-instruction.py java quarkus docker
```

For more details, see the [Agents README](.github/agents/README.md).

## Architecture

### Technology Stack

- **Language**: Java 21
- **Framework**: Quarkus 3.15.1
- **Build Tool**: Maven
- **Key Libraries**:
  - Structurizr (workspace modeling)
  - ADF Builder (Atlassian Document Format)
  - Playwright (browser automation)
  - Picocli (CLI framework)

### Application Structure

```
src/main/java/arnaudroubinet/structurizr/confluence/
├── cli/              # Command-line interface
├── client/           # REST clients (Structurizr, Confluence)
├── processor/        # Document format conversion
└── util/             # Utilities (SSL, ADF conversion)
```

## Development

### Building

```bash
# Full build with tests
mvn clean install

# Quick compilation check
mvn compile

# Run tests only
mvn test
```

### Docker Build

```bash
./build-docker.sh
```

This script:
1. Builds the Java application with Maven
2. Creates the Docker image
3. Tests the image functionality

### Running Tests

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=ConfluenceExporterIntegrationTest
```

## CI/CD

The project uses GitHub Actions for continuous integration and deployment:

- **Build & Test**: Runs on every push and pull request
- **Release**: Automated releases when version tags are pushed
- **Docker**: Publishes images to GitHub Container Registry

See `.github/workflows/` for workflow configurations.

## Configuration

### SSL Certificate Handling

For development environments with self-signed certificates:

```bash
# Disable SSL verification (development only!)
export DISABLE_SSL_VERIFICATION=true

# Or use command-line flag
java -jar app.jar export --disable-ssl-verification
```

⚠️ **Warning**: Never disable SSL verification in production.

For more details, see [SSL Bypass Documentation](docs/SSL_BYPASS.md).

### Authentication

The application supports multiple authentication methods:

- API tokens (recommended for Confluence Cloud)
- Basic authentication
- Environment variable configuration

See [Authentication Troubleshooting](docs/AUTHENTICATION_TROUBLESHOOTING.md) for details.

## Contributing

Contributions are welcome! Please read our development guidelines in [.github/copilot-instructions.md](.github/copilot-instructions.md) before contributing.

### Development Workflow

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Run tests: `mvn test`
5. Build: `mvn clean install`
6. Submit a pull request

### Code Style

The project follows standard Java conventions with:
- Java 21 modern features
- Quarkus patterns and best practices
- Comprehensive JavaDoc for public APIs
- Test coverage for critical paths

## Release Process

See [RELEASING.md](RELEASING.md) for detailed release instructions.

### Latest Release

**Version**: v1.0.20  
**Release Date**: September 30, 2025

Download the latest release from [GitHub Releases](https://github.com/arnaudroubinet/Structurizr-confluence/releases).

### Docker Images

Docker images are available at:
- `ghcr.io/arnaudroubinet/structurizr-confluence:latest`
- `ghcr.io/arnaudroubinet/structurizr-confluence:1.0.20`

## License

This project is licensed under the Apache License 2.0 - see the LICENSE file for details.

## Support

For issues, questions, or contributions:

- **Issues**: [GitHub Issues](https://github.com/arnaudroubinet/Structurizr-confluence/issues)
- **Discussions**: [GitHub Discussions](https://github.com/arnaudroubinet/Structurizr-confluence/discussions)
- **Documentation**: See the `docs/` directory

## Acknowledgments

- [Structurizr](https://structurizr.com/) - Architecture visualization tool
- [Quarkus](https://quarkus.io/) - Supersonic Subatomic Java
- [Atlassian](https://www.atlassian.com/) - Confluence platform
- [awesome-copilot](https://github.com/github/awesome-copilot) - Copilot instruction files

## Related Projects

- [Structurizr DSL](https://github.com/structurizr/dsl) - Text-based architecture modeling
- [Structurizr CLI](https://github.com/structurizr/cli) - Command-line tools for Structurizr
