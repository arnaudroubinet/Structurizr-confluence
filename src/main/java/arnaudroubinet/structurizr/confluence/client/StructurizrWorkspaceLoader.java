package arnaudroubinet.structurizr.confluence.client;

import com.structurizr.Workspace;
import com.structurizr.api.StructurizrClient;
import com.structurizr.api.StructurizrClientException;
import arnaudroubinet.structurizr.confluence.util.SslTrustUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.logging.Level;

/**
 * Client for loading workspaces from a Structurizr on-premise instance.
 */
public class StructurizrWorkspaceLoader {
    
    private static final Logger logger = LoggerFactory.getLogger(StructurizrWorkspaceLoader.class);
    
    private final StructurizrConfig config;
    private final StructurizrClient client;
    
    public StructurizrWorkspaceLoader(StructurizrConfig config) throws StructurizrClientException {
        this.config = config;
        
        // Configure SSL trust settings if needed
        if (SslTrustUtils.shouldDisableSslVerification()) {
            logger.warn("SSL certificate verification disabled for Structurizr client connections");
            SslTrustUtils.installTrustAllSslContext();
        }
        
        // Configure debug logging for HTTP calls if debug mode is enabled
        if (config.isDebugMode()) {
            logger.info("Debug mode enabled - HTTP request/response logging will be detailed");
            // Enable HTTP wire logging for debugging
            // Apache HttpClient 5 (used by StructurizrClient/WorkspaceApiClient) uses SLF4J for logging
            // In Quarkus, SLF4J is bridged to JBoss LogManager via slf4j-jboss-logmanager
            // We configure the JUL loggers (which are managed by JBoss LogManager in Quarkus)
            enableHttpLogging();
        }
        
        // Create StructurizrClient with API URL, key, and secret
        if (config.getApiUrl() != null && !config.getApiUrl().isEmpty()) {
            this.client = new StructurizrClient(config.getApiUrl(), config.getApiKey(), config.getApiSecret());
            if (config.isDebugMode()) {
                logger.debug("Created StructurizrClient for API URL: {}", config.getApiUrl());
            }
        } else {
            // Use default Structurizr cloud URL if no API URL provided
            this.client = new StructurizrClient(config.getApiKey(), config.getApiSecret());
            if (config.isDebugMode()) {
                logger.debug("Created StructurizrClient for Structurizr cloud service");
            }
        }
    }
    
    /**
     * Enables HTTP logging for Apache HttpClient 5 used by StructurizrClient/WorkspaceApiClient.
     * 
     * In Quarkus applications, logging is managed by JBoss LogManager which implements the
     * java.util.logging (JUL) API. Apache HttpClient 5 uses SLF4J for logging, which is
     * bridged to JBoss LogManager via slf4j-jboss-logmanager.
     * 
     * This method configures the underlying JUL loggers (which are actually JBoss LogManager
     * loggers) to show HTTP request/response details at runtime when debug mode is enabled.
     * 
     * @see <a href="https://quarkus.io/guides/logging">Quarkus Logging Guide</a>
     */
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
    
    /**
     * Loads a workspace from the Structurizr instance.
     * 
     * @return the loaded workspace
     * @throws StructurizrClientException if loading fails
     */
    public Workspace loadWorkspace() throws StructurizrClientException {
        logger.info("Loading workspace {} from Structurizr instance", config.getWorkspaceId());
        
        if (config.isDebugMode()) {
            logger.debug("Debug mode: Making HTTP request to load workspace {}", config.getWorkspaceId());
            logger.debug("Debug mode: API URL: {}", config.getApiUrl() != null ? config.getApiUrl() : "Structurizr Cloud");
            logger.debug("Debug mode: API Key: {}...", config.getApiKey() != null && config.getApiKey().length() > 4 
                ? config.getApiKey().substring(0, 4) + "*****" : "null");
        }
        
        long startTime = System.currentTimeMillis();
        Workspace workspace;
        
        try {
            workspace = client.getWorkspace(config.getWorkspaceId());
        } catch (StructurizrClientException e) {
            if (config.isDebugMode()) {
                logger.debug("Debug mode: HTTP request failed after {} ms", System.currentTimeMillis() - startTime);
                logger.debug("Debug mode: Exception details: {}", e.getMessage(), e);
            }
            throw e;
        }
        
        long endTime = System.currentTimeMillis();
        
        if (workspace == null) {
            if (config.isDebugMode()) {
                logger.debug("Debug mode: HTTP request completed in {} ms but returned null workspace", endTime - startTime);
            }
            throw new RuntimeException("Failed to load workspace " + config.getWorkspaceId());
        }
        
        if (config.isDebugMode()) {
            logger.debug("Debug mode: HTTP request completed successfully in {} ms", endTime - startTime);
            logger.debug("Debug mode: Workspace loaded - Name: '{}', ID: {}", workspace.getName(), config.getWorkspaceId());
            if (workspace.getModel() != null) {
                logger.debug("Debug mode: Workspace contains {} software systems", 
                    workspace.getModel().getSoftwareSystems().size());
            }
        }
        
        logger.info("Successfully loaded workspace '{}' (ID: {})", 
            workspace.getName(), config.getWorkspaceId());
        
        return workspace;
    }
}