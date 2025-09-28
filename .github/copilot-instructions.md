# Copilot Coding Agent Instructions

## Project Overview

**Structurizr Confluence Exporter** is a Java client that exports Structurizr workspace documentation and Architecture Decision Records (ADRs) to Confluence Cloud in Atlassian Document Format (ADF). 

### Key Project Information
- **Documentation**: Always use context7 mcp server when you use library or external concepts
- **Language**: Java 21 
- **Build Tool**: Maven 3.6+


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

### Standard Build Process

⚠️ **CRITICAL**: Always run the full build before finalizing changes:

`mvn --no-transfer-progress clean install`