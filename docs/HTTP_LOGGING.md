# HTTP Logging for WorkspaceApiClient

## Overview

This document explains how HTTP call logging works for the `WorkspaceApiClient` (via `StructurizrClient`) when debug mode is enabled.

## Problem

Previously, when debug mode was enabled, HTTP calls made by the `WorkspaceApiClient` (or `StructurizrClient` which extends it) were not visible in the logs. The code attempted to configure Apache Commons Logging, but the underlying HTTP client (Apache HttpClient 5) uses SLF4J for logging, which wasn't being properly configured.

## Solution

The `StructurizrWorkspaceLoader` class now properly configures HTTP logging when debug mode is enabled by:

1. **Configuring java.util.logging (JUL) loggers** - Since Apache HttpClient 5 uses SLF4J, and Quarkus bridges SLF4J to JBoss LogManager (which uses JUL), we need to configure the underlying JUL loggers.

2. **Setting appropriate log levels** - The following loggers are set to `Level.FINE` (equivalent to DEBUG):
   - `org.apache.hc.client5.http` - General HTTP client operations
   - `org.apache.hc.client5.http.wire` - HTTP wire traffic (requests/responses)
   - `org.apache.hc.client5.http.headers` - HTTP headers

## Usage

To enable HTTP logging for `WorkspaceApiClient`/`StructurizrClient`, simply enable debug mode when creating a `StructurizrConfig`:

```java
StructurizrConfig config = new StructurizrConfig(
    "http://localhost:8080/api",  // API URL
    "your-api-key",                // API key
    "your-api-secret",             // API secret
    12345L,                        // Workspace ID
    true                           // Debug mode enabled
);

StructurizrWorkspaceLoader loader = new StructurizrWorkspaceLoader(config);
```

When debug mode is enabled, you will see detailed HTTP request/response information in the logs, including:
- HTTP request methods and URLs
- Request and response headers
- Request and response bodies
- Connection details
- Timing information

## Technical Details

### Apache HttpClient 5 Logging

Apache HttpClient 5 uses SLF4J for logging with the following logger hierarchy:

- `org.apache.hc.client5.http` - Main HTTP client logger
  - `org.apache.hc.client5.http.wire` - Wire-level HTTP traffic (raw requests/responses)
  - `org.apache.hc.client5.http.headers` - HTTP header information

### Quarkus Logging Bridge

In Quarkus applications, the logging architecture is:

1. **JBoss LogManager** (version 3.0.6.Final) is the core logging implementation
2. **SLF4J is bridged to JBoss LogManager** via `slf4j-jboss-logmanager` (version 2.0.0.Final)
3. **JBoss LogManager implements the JUL API** - it extends `java.util.logging.LogManager`
4. **Apache HttpClient 5 uses SLF4J** for logging

When we call `java.util.logging.Logger.getLogger()` in Quarkus, we actually get an `org.jboss.logmanager.Logger` instance managed by JBoss LogManager. This is why our code uses the standard JUL API but still properly integrates with the Quarkus/JBoss logging system.

The logging flow for Apache HttpClient 5 is:
```
HttpClient 5 → SLF4J → slf4j-jboss-logmanager bridge → JBoss LogManager → Console/File output
```

### Code Implementation

The `enableHttpLogging()` method in `StructurizrWorkspaceLoader`:

```java
private void enableHttpLogging() {
    try {
        // Get the JUL loggers for Apache HttpClient 5
        // Note: In Quarkus, java.util.logging.Logger.getLogger() returns
        // org.jboss.logmanager.Logger instances which are managed by JBoss LogManager
        java.util.logging.Logger httpClientLogger = java.util.logging.Logger.getLogger("org.apache.hc.client5.http");
        java.util.logging.Logger wireLogger = java.util.logging.Logger.getLogger("org.apache.hc.client5.http.wire");
        java.util.logging.Logger headersLogger = java.util.logging.Logger.getLogger("org.apache.hc.client5.http.headers");
        
        // Set all to FINE (equivalent to DEBUG in SLF4J)
        // These calls work with JBoss LogManager which manages the actual log levels
        httpClientLogger.setLevel(Level.FINE);
        wireLogger.setLevel(Level.FINE);
        headersLogger.setLevel(Level.FINE);
        
        logger.debug("HTTP client logging enabled for WorkspaceApiClient/StructurizrClient");
    } catch (Exception e) {
        logger.warn("Failed to enable HTTP client logging: {}", e.getMessage());
    }
}
```

This approach properly integrates with Quarkus's embedded JBoss LogManager without requiring any Quarkus-specific or JBoss-specific APIs.

## Testing

Unit tests are provided in `StructurizrWorkspaceLoaderTest` to verify:
- HTTP logging is enabled when debug mode is true
- HTTP logging is not enabled when debug mode is false
- `StructurizrClient` is created successfully for both on-premise and cloud scenarios

## Example Log Output

When debug mode is enabled, you will see log output similar to:

```
11:42:18 INFO  [StructurizrWorkspaceLoader] Debug mode enabled - HTTP request/response logging will be detailed
11:42:18 DEBUG [StructurizrWorkspaceLoader] Created StructurizrClient for API URL: http://localhost:8080/api
11:42:18 DEBUG [StructurizrWorkspaceLoader] HTTP client logging enabled for WorkspaceApiClient/StructurizrClient
11:42:18 DEBUG [StructurizrWorkspaceLoader] Debug mode: Making HTTP request to load workspace 12345
11:42:18 FINE  [org.apache.hc.client5.http.wire] >> GET /api/workspace/12345 HTTP/1.1
11:42:18 FINE  [org.apache.hc.client5.http.headers] >> User-Agent: Structurizr-Java/1.29.0
11:42:18 FINE  [org.apache.hc.client5.http.wire] << HTTP/1.1 200 OK
11:42:18 DEBUG [StructurizrWorkspaceLoader] Debug mode: HTTP request completed successfully in 234 ms
```

## Configuration

The default log levels for HTTP client packages are defined in `application.properties`:

```properties
# HTTP Client logging configuration (used by WorkspaceApiClient/StructurizrClient)
# These are disabled by default and only enabled when debug mode is active
# Apache HttpClient 5 uses SLF4J for logging
quarkus.log.category."org.apache.hc.client5.http".level=INFO
quarkus.log.category."org.apache.hc.client5.http.wire".level=INFO
quarkus.log.category."org.apache.hc.client5.http.headers".level=INFO
```

These settings are overridden at runtime when debug mode is enabled.

## Related Classes

- `StructurizrWorkspaceLoader` - Main class that configures HTTP logging
- `StructurizrConfig` - Configuration object that includes debug mode flag
- `StructurizrClient` - Structurizr library class that extends `WorkspaceApiClient`
- `WorkspaceApiClient` - Structurizr library class that performs HTTP operations
