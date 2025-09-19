# Copilot Coding Agent Instructions

## Project Overview

**Structurizr Confluence Exporter** is a Java library that exports Structurizr workspace documentation and Architecture Decision Records (ADRs) to Confluence Cloud in Atlassian Document Format (ADF). The project can load workspaces from Structurizr on-premise instances or work with provided workspace objects.

### Key Project Information
- **Documentation**: Always use context7 mcp server when you use library or external concepts
- **Language**: Java 21 
- **Build Tool**: Maven 3.6+
- **Project Type**: Library JAR (1.0.0)
- **Main Framework**: Structurizr for Java
- **Confluence Format**: ADF (Atlassian Document Format)
- **Target Platform**: Confluence Cloud API v2
- **Repository Size**: Small/Medium (~20 Java classes, comprehensive test suite)

## MCP Context7 Configuration

This repository is configured with Context7 MCP server for accessing up-to-date library documentation and examples. The configuration enables agents to retrieve current documentation for external libraries and frameworks.

### MCP Server Setup
- **Configuration File**: `.vscode/settings.json` and `.mcp-config.json`
- **Server URL**: `https://mcp.context7.com/mcp`
- **Environment Variable**: Set `CONTEXT7_API_KEY` with your API key
- **Available Tools**: `get-library-docs`, `resolve-library-id`

### Usage Guidelines
- **ALWAYS** use Context7 MCP when working with external libraries
- Query for library documentation before implementing features
- Use specific library versions when available
- Prefer Context7 over web searches for technical documentation

### Accessing Atlassian Documentation with Context7
To find Atlassian (Confluence, ADF, REST API) documentation using Context7:

1. **Resolve Library ID**: Use `mcp_context7_resolve-library-id` with "atlassian" or "confluence"
2. **Key Library IDs for this project**:
   - Confluence REST API: `/websites/atlassian-atlassian-confluence-rest-6.6.0`
   - General Confluence docs: `/websites/confluence_atlassian_spaces_doc`
   - Atlassian Forge (ADF): `/websites/developer_atlassian_com-platform-forge`
3. **Get Documentation**: Use `mcp_context7_get-library-docs` with the resolved library ID
4. **Focus Topics**: Specify topics like "ADF", "REST API", "document format" for targeted results

## Build and Validation Instructions

### Prerequisites
- **Java 21** (required - configured in pom.xml)
- **Maven 3.6+**
- **GraphViz** (for diagram generation - installed in CI)
- **Internet connection** (for dependency downloads and optional integration tests)

### Standard Build Process

⚠️ **CRITICAL**: Always run the full build before finalizing changes:

```bash
# Primary build command - use this for all builds
./scripts/build.sh
```

This script executes: `mvn -B --no-transfer-progress clean install`

### Alternative Build Commands

```bash
# Manual Maven build (equivalent to build script)
mvn clean install

# Compile only
mvn clean compile

# Package without tests
mvn clean package -DskipTests
```

### Testing Strategy

The project has **two distinct types of tests** with different requirements:

#### 1. Unit Tests (Always Required) ✅
```bash
# Run ADF unit tests - MUST ALWAYS PASS
mvn test -Dtest=AsciiDocConverterTest

# These tests validate ADF document generation
# No network access required
# Must be green before any PR
```

#### 2. Integration Tests (Credential-Dependent) 🔐
```bash
# Run integration tests
mvn test -Dtest=ConfluenceExporterIntegrationTest

# Tests real Confluence Cloud integration
# Target: https://arnaudroubinet.atlassian.net
# Space: Test
```

This script:
1. Compiles the project
2. Runs unit tests (must pass)
3. Checks for integration test credentials
4. Runs integration tests if credentials available
5. Provides clear success/failure feedback

### Known Build Issues and Workarounds

1. **Confluence API Rate Limits**: Integration tests may fail with rate limiting
   - Workaround: Wait 1-2 minutes between test runs
   - Use unit tests for rapid development cycles

## Project Architecture and Layout

### Core Architecture
```
src/main/java/com/structurizr/confluence/
├── ConfluenceExporter.java          # Main entry point class
├── StructurizrOnPremiseExample.java  # Usage example
├── client/                          # Confluence & Structurizr API clients
│   ├── ConfluenceClient.java        # Confluence Cloud API wrapper
│   ├── ConfluenceConfig.java        # Confluence connection config
│   ├── StructurizrConfig.java       # Structurizr on-premise config
│   └── StructurizrWorkspaceLoader.java # Workspace loading logic
└── processor/                       # Document processing
    ├── AsciiDocConverter.java       # AsciiDoc to HTML conversion
    └── HtmlToAdfConverter.java      # HTML to ADF conversion
```

### Test Structure
```
src/test/java/com/structurizr/confluence/
├── ConfluenceExporterIntegrationTest.java # Integration tests
└── processor/
    ├── AsciiDocConverterTest.java         # Unit tests (ADF)
    └── AsciiDocDebugTest.java             # Debug utilities
```

### Configuration Files
- **pom.xml**: Maven configuration, Java 21, dependencies
- **package.json**: Playwright E2E tests (minimal)
- **renovate.json**: Dependency management automation
- **.github/workflows/ci.yml**: CI/CD pipeline
- **.vscode/settings.json**: VS Code and MCP configuration
- **.mcp-config.json**: General MCP server configuration
- **MCP-SETUP.md**: Detailed MCP setup instructions

### Build Artifacts
- **target/structurizr-confluence-1.0.0.jar**: Main library JAR
- **target/surefire-reports/**: Test execution reports
- **target/classes/**: Compiled Java classes

## Dependencies and Frameworks

### Core Dependencies
- **Structurizr for Java** (1.29.0): Core architecture modeling
- **Atlassian ADF Builder** (3.0.3): Official ADF document generation
- **Jackson** (2.15.2): JSON processing
- **Apache HttpClient** (4.5.14): Confluence API communication
- **AsciidoctorJ** (2.5.10): AsciiDoc processing
- **SLF4J + Logback**: Logging framework

### Test Dependencies
- **JUnit 5** (5.10.0): Unit testing framework
- **Mockito** (5.5.0): Mocking framework
- **Playwright**: E2E testing (optional)

## CI/CD Pipeline

### GitHub Actions Workflow (.github/workflows/ci.yml)
```yaml
# Triggers: All branches push, PRs to main
# Environment: Ubuntu latest, Java 21
# Steps:
1. Checkout code
2. Setup JDK 21 with Maven cache
3. Install GraphViz dependency
4. Execute ./scripts/build.sh
```

### Validation Requirements
1. **Java 21 compatibility**: Code must compile and run on Java 21
2. **Unit tests must pass**: ADF generation tests are mandatory
3. **Maven build success**: `mvn clean install` must complete successfully
4. **GraphViz availability**: Required for diagram processing

## Common Development Patterns

### Adding New Features
1. Create feature branch: `git checkout -b feature/your-feature`
2. Implement changes in appropriate package (client/ or processor/)
3. Add unit tests in corresponding test directory
4. Run `./test.sh` to validate changes
5. Ensure `./scripts/build.sh` passes completely

### Integration Test Development
- Set environment variables for Confluence access
- Target test space: `https://arnaudroubinet.atlassian.net/wiki/spaces/Test/`
- Page updates target specific page ID: 10977556
- Include screenshots in PR for validation

### Code Quality Requirements
- Follow existing package structure
- Use SLF4J for logging
- Handle exceptions appropriately (see ConfluenceClient examples)
- Maintain backwards compatibility for public API methods

## Debugging and Troubleshooting

### Common Issues
1. **Build failures**: Check Java version (must be 21)
2. **Test failures**: Verify network connectivity for integration tests
3. **ADF generation errors**: Check AsciiDoc content formatting
4. **Confluence API errors**: Verify credentials and API token validity

### Debug Tools
- **AsciiDocDebugTest.java**: Manual AsciiDoc conversion testing
- **test.sh**: Comprehensive validation script
- **Maven debug**: Use `-X` flag for verbose output

## Trust These Instructions

**⚠️ IMPORTANT**: These instructions are comprehensive and validated. Only perform additional searches if:
1. Information is incomplete or contradictory
2. Errors occur that aren't documented here
3. New features require understanding not covered

The build process, test requirements, and project structure are thoroughly documented above. Trust this guidance for efficient development.

---

## Quick Reference Commands

```bash
# Standard development workflow
./scripts/build.sh                    # Full build
./test.sh                             # Comprehensive testing
mvn test -Dtest=AsciiDocConverterTest # Unit tests only
mvn clean compile                     # Compile check

# Integration testing
export CONFLUENCE_USER="email"
export CONFLUENCE_TOKEN="token"
mvn test -Dtest=ConfluenceExporterIntegrationTest
```