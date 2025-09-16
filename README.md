# Structurizr Confluence Exporter

A Java library that exports [Structurizr](https://structurizr.com/) workspace documentation and ADRs to Confluence Cloud in Atlassian Document Format (ADF). Can load workspaces from Structurizr on-premise instances or work with provided workspace objects.

## Features

- Exports Structurizr workspace documentation to Confluence Cloud
- **Exports Architecture Decision Records (ADRs)** from Structurizr workspaces
- **Loads workspaces from Structurizr on-premise instances** using the official Structurizr client
- Generates documentation in Atlassian Document Format (ADF) using official library
- Creates structured pages with table of contents
- Supports all Structurizr model elements (People, Software Systems, Containers, Components)
- Supports all Structurizr view types (System Landscape, System Context, Container, Component, Deployment)
- Automatically creates or updates pages in Confluence

## Requirements

- Java 11 or higher
- Confluence Cloud instance
- Confluence API token
- (Optional) Structurizr on-premise instance with API access

## Installation

Add the dependency to your Maven project:

```xml
<dependency>
    <groupId>com.structurizr</groupId>
    <artifactId>structurizr-confluence</artifactId>
    <version>1.0.0</version>
</dependency>
```

## Usage

### Basic Usage

```java
import com.structurizr.Workspace;
import com.structurizr.confluence.ConfluenceExporter;
import com.structurizr.confluence.client.ConfluenceConfig;

// Create your Structurizr workspace
Workspace workspace = new Workspace("My Architecture", "Description of my software architecture");
// ... populate your workspace with model elements and views

// Configure Confluence connection
ConfluenceConfig config = new ConfluenceConfig(
    "https://your-domain.atlassian.net", // Confluence base URL
    "your-email@example.com",            // Your email
    "your-api-token",                    // Your API token
    "SPACE"                              // Space key where pages will be created
);

// Export to Confluence
ConfluenceExporter exporter = new ConfluenceExporter(config);
try {
    exporter.export(workspace);
    System.out.println("Workspace exported successfully to Confluence!");
} catch (Exception e) {
    System.err.println("Export failed: " + e.getMessage());
}
```

### Loading from Structurizr On-Premise

```java
import com.structurizr.confluence.ConfluenceExporter;
import com.structurizr.confluence.client.ConfluenceConfig;
import com.structurizr.confluence.client.StructurizrConfig;

// Configure Structurizr on-premise connection
StructurizrConfig structurizrConfig = new StructurizrConfig(
    "https://your-structurizr-instance.com",  // On-premise Structurizr URL
    "your-api-key",                           // Your API key
    "your-api-secret",                        // Your API secret
    12345L                                    // Workspace ID to load
);

// Configure Confluence connection
ConfluenceConfig confluenceConfig = new ConfluenceConfig(
    "https://your-domain.atlassian.net",     // Confluence base URL
    "your-email@example.com",                // Your email
    "your-api-token",                        // Your API token
    "SPACE"                                  // Space key
);

// Export workspace (including documentation and ADRs) from Structurizr to Confluence
ConfluenceExporter exporter = new ConfluenceExporter(confluenceConfig, structurizrConfig);
try {
    exporter.exportFromStructurizr();
    System.out.println("Workspace and ADRs exported successfully!");
} catch (Exception e) {
    System.err.println("Export failed: " + e.getMessage());
}
```

### Getting Confluence API Token

1. Go to your Atlassian account settings
2. Navigate to Security → Create and manage API tokens
3. Create a new API token
4. Use your email and the API token for authentication

### Generated Documentation Structure

The exporter creates the following page structure in Confluence:

```
📄 [Workspace Name] - Architecture Documentation (main page)
├── 📄 System Landscape Views
├── 📄 System Context Views  
├── 📄 Container Views
├── 📄 Component Views
├── 📄 Deployment Views
├── 📄 Model Documentation
└── 📄 Architecture Decision Records (if ADRs exist)
    ├── 📄 ADR 001 - [Decision Title]
    ├── 📄 ADR 002 - [Decision Title]
    └── 📄 ...
```

Each page contains:
- Structured content in ADF format
- Table of contents
- View descriptions and metadata
- Model element documentation with properties
- **ADR pages with decision metadata and links** (when available)

## ADF (Atlassian Document Format)

This library uses Atlassian's official ADF builder library (`com.atlassian.stride:adf-builder`) to generate documentation in ADF, which is the native format used by Confluence Cloud. ADF provides:

- Rich text formatting
- Structured content (headings, paragraphs, lists)
- Code blocks and inline code
- Links and references

## Dependencies

- [Structurizr for Java](https://github.com/structurizr/java) - Core Structurizr library
- [Atlassian ADF Builder](https://bitbucket.org/atlassian/adf-builder-java) - Official ADF document generation
- Jackson - JSON processing for ADF serialization
- Apache HttpClient - HTTP communication with Confluence API
- SLF4J - Logging

## Testing

Integration tests cover ADF document generation using the official library. The implementation demonstrates proper usage of Atlassian's ADF builder and validates JSON serialization compatibility with Confluence Cloud.

### Running Integration Tests

To run the full integration test that exports to a real Confluence instance:

1. Set environment variables:
   ```bash
   export CONFLUENCE_USER="your-email@example.com"
   export CONFLUENCE_TOKEN="your-confluence-api-token"
   ```

2. Run the integration test:
   ```bash
   mvn test -Dtest=ConfluenceIntegrationTest
   ```

The integration test will:
- Create a Financial Risk System workspace based on Structurizr examples
- Export it to `https://arnaudroubinet.atlassian.net` in the Test space
- Validate the markdown content from the quality attributes example
- Verify successful export without errors

### Testing Specific Page Updates

To test updating a specific Confluence page by ID:

```bash
export CONFLUENCE_USER="your-email@example.com"
export CONFLUENCE_TOKEN="your-confluence-api-token"
mvn test -Dtest=ConfluenceIntegrationTest#shouldUpdateSpecificConfluencePage
```

This test:
- Updates Confluence page ID 10977556 directly
- Uses simple ADF content with workspace information
- Tests the direct page update functionality
- Validates the page ID is correctly returned

## License

This project is licensed under the Apache License 2.0.

## Contributing

Contributions are welcome! Please feel free to submit issues and pull requests.