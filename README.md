# Structurizr Confluence Exporter

A Java library that exports [Structurizr](https://structurizr.com/) workspace documentation to Confluence Cloud in Atlassian Document Format (ADF).

## Features

- Exports Structurizr workspace documentation to Confluence Cloud
- Generates documentation in Atlassian Document Format (ADF)
- Creates structured pages with table of contents
- Supports all Structurizr model elements (People, Software Systems, Containers, Components)
- Supports all Structurizr view types (System Landscape, System Context, Container, Component, Deployment)
- Automatically creates or updates pages in Confluence

## Requirements

- Java 11 or higher
- Confluence Cloud instance
- Confluence API token

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
└── 📄 Model Documentation
```

Each page contains:
- Structured content in ADF format
- Table of contents
- View descriptions and metadata
- Model element documentation with properties

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

## License

This project is licensed under the Apache License 2.0.

## Contributing

Contributions are welcome! Please feel free to submit issues and pull requests.