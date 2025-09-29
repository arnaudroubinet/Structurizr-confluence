# Copilot Coding Agent Instructions

## Project Overview

**Structurizr Confluence Exporter** is a Java client that exports Structurizr workspace documentation and Architecture Decision Records (ADRs) to Confluence Cloud in Atlassian Document Format (ADF). 

This application enables organizations to automatically synchronize their software architecture documentation from Structurizr workspaces into Confluence Cloud, making it accessible to broader teams through a familiar collaboration platform.

### Key Project Information
- **Language**: Java 21 (Required - leverages modern Java features)
- **Build Tool**: Maven 3.6+
- **Framework**: Quarkus 3.15.1 (for CLI and dependency injection)
- **Architecture**: CLI application with REST client integrations
- **Documentation**: Always use Context7 MCP server for external library documentation

### Architecture Overview
The application follows a modular architecture:
- **CLI Layer** (`src/main/java/com/structurizr/confluence/cli/`): Command-line interface using Picocli
- **Export Engine** (`src/main/java/com/structurizr/confluence/`): Core export logic and orchestration
- **Client Layer** (`src/main/java/com/structurizr/confluence/client/`): REST clients for Structurizr and Confluence APIs
- **Processing Layer** (`src/main/java/com/structurizr/confluence/processor/`): Document format conversion and processing
- **Utilities** (`src/main/java/com/structurizr/confluence/util/`): SSL configuration, ADF conversion, etc.

### Key Dependencies
- **Structurizr**: Core library (1.29.0) for workspace modeling and client API
- **ADF Builder**: Atlassian Document Format generation (3.0.3)
- **Quarkus**: Application framework for CLI and REST clients
- **Playwright**: Browser automation for diagram export
- **Jackson**: JSON processing for API responses
- **AST/Markdown processors**: Content format conversion

### Usage Guidelines
- **ALWAYS** use Context7 MCP when working with external libraries
- Query for library documentation before implementing features
- Use specific library versions when available
- Prefer Context7 over web searches for technical documentation

### Accessing Atlassian Documentation with Context7
To find Atlassian (Confluence, ADF, REST API) documentation using Context7:

1. **Resolve Library ID**: Use `context7-resolve-library-id` with "atlassian" or "confluence"
2. **Key Library IDs for this project**:
   - Confluence REST API: `/websites/atlassian-atlassian-confluence-rest-6.6.0`
   - General Confluence docs: `/websites/confluence_atlassian_spaces_doc`
   - Atlassian Forge (ADF): `/websites/developer_atlassian_com-platform-forge`
3. **Get Documentation**: Use `context7-get-library-docs` with the resolved library ID
4. **Focus Topics**: Specify topics like "ADF", "REST API", "document format" for targeted results

## Environment Setup

### Prerequisites
- **Java 21**: Use Temurin distribution for consistency with CI/CD
- **Maven 3.6+**: For building and dependency management
- **Docker** (optional): For containerized execution and testing
- **Playwright browsers**: Automatically installed during build for diagram export

### Setting Java 21
```bash
export JAVA_HOME=/usr/lib/jvm/temurin-21-jdk-amd64
export PATH=$JAVA_HOME/bin:$PATH
```

### Installing Playwright Dependencies
```bash
mvn exec:java -e -D exec.mainClass=com.microsoft.playwright.CLI -D exec.args="install --with-deps"
```

## Build and Validation Instructions

### Standard Build Process

⚠️ **CRITICAL**: Always run the full build before finalizing changes:

```bash
mvn --no-transfer-progress clean install
```

### Build Variants

**Quick compilation check:**
```bash
mvn --no-transfer-progress clean compile
```

**Run tests only:**
```bash
mvn --no-transfer-progress test
```

**Full build with verification:**
```bash
mvn --no-transfer-progress clean install
```

**Docker build:**
```bash
./build-docker.sh
```

### CI/CD Integration
- The project uses GitHub Actions for CI/CD (`.github/workflows/ci.yml`)
- All commits are built and tested automatically
- Docker image validation is included in CI pipeline
- Java 21 and Playwright dependencies are pre-configured in CI environment

## Testing Guidelines

### Test Structure
- **Integration Tests**: `ConfluenceExporterIntegrationTest` - Full workflow testing with real Confluence API
- **Unit Tests**: Component-specific tests in `src/test/java/com/structurizr/confluence/processor/`
- **Utility Tests**: SSL configuration, format conversion, and utility functions

### Test Patterns
1. **Use descriptive test names** that explain the scenario being tested
2. **Follow AAA pattern**: Arrange, Act, Assert
3. **Mock external dependencies** except in integration tests
4. **Use TestContainers** for services requiring real infrastructure
5. **Include success and failure scenarios**

### Running Tests
```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=ConfluenceExporterIntegrationTest

# Run with verbose output
mvn test -X
```

### Test Environment Variables
For integration tests, set these environment variables:
- `CONFLUENCE_URL`: Target Confluence instance
- `CONFLUENCE_USER`: User email for authentication
- `CONFLUENCE_TOKEN`: API token for authentication
- `CONFLUENCE_SPACE_KEY`: Target space for testing

## Code Style and Standards

### Java Coding Standards
- **Use Java 21 features** where appropriate (records, pattern matching, etc.)
- **Follow standard Java naming conventions**
- **Use meaningful variable and method names**
- **Prefer composition over inheritance**
- **Use dependency injection** (Quarkus CDI) for loosely coupled design

### Documentation Standards
- **JavaDoc for public APIs** - especially for exported classes and methods
- **Inline comments for complex logic** - explain the "why", not the "what"
- **README updates** for significant feature changes
- **ADR documentation** for architectural decisions

### Error Handling
- **Use specific exception types** rather than generic RuntimeException
- **Log errors with context** using SLF4J
- **Provide meaningful error messages** for end users
- **Handle SSL certificate issues** gracefully (see `docs/SSL_BYPASS.md`)

## Security Considerations

### SSL Certificate Handling
- **Default**: Strict SSL verification enabled
- **Development**: SSL bypass available via `--disable-ssl-verification` flag
- **Environment**: `DISABLE_SSL_VERIFICATION=true` for automated environments
- **Security Warning**: SSL bypass should NEVER be used in production with untrusted certificates

### Credential Management
- **Use environment variables** for sensitive data (API tokens, passwords)
- **Never hardcode credentials** in source code
- **Support multiple authentication methods** where applicable
- **Log credential usage** without exposing values

### API Integration Security
- **Validate all external inputs** from Structurizr and Confluence APIs
- **Use HTTPS for all external communications**
- **Implement proper timeout handling** for external API calls
- **Rate limiting awareness** for API calls

## Common Issues and Troubleshooting

### Build Issues
1. **Java version mismatch**: Ensure Java 21 is active (`java -version`)
2. **Playwright browser download failures**: Run browser install manually
3. **Memory issues**: Increase Maven memory: `export MAVEN_OPTS="-Xmx2g"`

### Runtime Issues
1. **SSL certificate errors**: Use `--disable-ssl-verification` for development only
2. **Confluence authentication failures**: Verify API token and permissions
3. **Diagram export failures**: Ensure Playwright browsers are installed
4. **Memory issues with large workspaces**: Increase JVM heap size

### Debugging
- **Enable debug logging**: Use `--debug` flag or set log level in `application.properties`
- **Network debugging**: Use `--trace` for detailed HTTP request/response logging
- **Playwright debugging**: Set `PWDEBUG=1` environment variable

## Docker Usage

### Building Docker Image
```bash
./build-docker.sh
```

### Running in Docker
```bash
docker run --rm \
  -e CONFLUENCE_URL=https://your-domain.atlassian.net \
  -e CONFLUENCE_USER=your-email@domain.com \
  -e CONFLUENCE_TOKEN=your-api-token \
  -e CONFLUENCE_SPACE_KEY=YOUR_SPACE \
  structurizr-confluence:latest export --workspace-id 123
```

### Docker Development
- **Image size**: ~1.05GB (includes Java 21 + Playwright dependencies)
- **Base image**: `eclipse-temurin:21-jre`
- **Optimization**: Multi-stage build for minimal production image
- **Browser support**: Full Playwright browser stack included

## Contributing Guidelines

### Before Making Changes
1. **Read existing code** to understand patterns and conventions
2. **Run full test suite** to ensure baseline functionality
3. **Check for existing issues** or related work
4. **Review recent commits** to understand current development direction

### Making Changes
1. **Create focused commits** with descriptive messages
2. **Add tests for new functionality**
3. **Update documentation** for user-facing changes
4. **Follow existing code organization** and naming patterns
5. **Test with real Confluence instance** when possible

### Pull Request Process
1. **Ensure all tests pass** locally before pushing
2. **Include integration test verification** for significant changes
3. **Update documentation** if APIs or usage patterns change
4. **Verify Docker build** if dependencies or build process changes