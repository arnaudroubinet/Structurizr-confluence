package com.structurizr.confluence.client;

import com.structurizr.Workspace;
import com.structurizr.api.WorkspaceApiClient;
import com.structurizr.api.StructurizrClientException;
import com.structurizr.confluence.util.SslTrustUtils;
import com.fasterxml.jackson.core.JsonParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Client for loading workspaces from a Structurizr on-premise instance.
 */
public class StructurizrWorkspaceLoader {
    
    private static final Logger logger = LoggerFactory.getLogger(StructurizrWorkspaceLoader.class);
    
    private final StructurizrConfig config;
    private final WorkspaceApiClient client;
    
    public StructurizrWorkspaceLoader(StructurizrConfig config) throws StructurizrClientException {
        this.config = config;
        
        // Configure SSL trust settings if needed
        if (SslTrustUtils.shouldDisableSslVerification()) {
            logger.warn("SSL certificate verification disabled for Structurizr client connections");
            SslTrustUtils.installTrustAllSslContext();
        }
        
        // Create WorkspaceApiClient with API URL, key, and secret
        if (config.getApiUrl() != null && !config.getApiUrl().isEmpty()) {
            this.client = new WorkspaceApiClient(config.getApiUrl(), config.getApiKey(), config.getApiSecret());
            logger.info("Configured Structurizr client for on-premise instance: {}", config.getApiUrl());
        } else {
            // Use default Structurizr cloud URL if no API URL provided
            this.client = new WorkspaceApiClient(config.getApiKey(), config.getApiSecret());
            logger.info("Configured Structurizr client for cloud service");
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
        
        try {
            Workspace workspace = client.getWorkspace(config.getWorkspaceId());
            
            if (workspace == null) {
                throw new RuntimeException("Failed to load workspace " + config.getWorkspaceId() + 
                    " - received null response from Structurizr API");
            }
            
            logger.info("Successfully loaded workspace '{}' (ID: {})", 
                workspace.getName(), config.getWorkspaceId());
            
            return workspace;
        } catch (StructurizrClientException e) {
            // Check if this is a JSON parsing issue (common with reverse proxy problems)
            if (e.getCause() instanceof JsonParseException || 
                (e.getMessage() != null && e.getMessage().contains("Unexpected character ('<'"))) {
                
                String errorMsg = "Failed to parse JSON response from Structurizr API. " +
                    "This usually indicates that the server returned HTML (possibly an error page) instead of JSON. " +
                    "Common causes when using Structurizr on-premise behind a reverse proxy:\n" +
                    "1. Reverse proxy configuration issues - check that the API endpoint " + 
                    (config.getApiUrl() != null ? config.getApiUrl() : "https://api.structurizr.com") + 
                    " is correctly configured\n" +
                    "2. Authentication issues - verify API key and secret are correct\n" +
                    "3. Workspace ID " + config.getWorkspaceId() + " does not exist or is not accessible\n" +
                    "4. SSL certificate issues - try using --disable-ssl-verification flag if using self-signed certificates\n" +
                    "5. Check reverse proxy logs for authentication or routing errors\n" +
                    "Original error: " + e.getMessage();
                
                logger.error("JSON parsing error when loading workspace (likely reverse proxy issue): {}", errorMsg);
                throw new RuntimeException(errorMsg, e);
            }
            
            // Re-throw StructurizrClientException with additional context  
            String errorMsg = "Failed to load workspace " + config.getWorkspaceId() + 
                " from Structurizr instance " + 
                (config.getApiUrl() != null ? config.getApiUrl() : "cloud service") + 
                ". Error: " + e.getMessage();
            
            logger.error("StructurizrClientException when loading workspace: {}", errorMsg);
            throw e;  // Re-throw the original exception
        } catch (Exception e) {
            // Catch any other unexpected exceptions
            String errorMsg = "Unexpected error when loading workspace " + config.getWorkspaceId() + 
                " from Structurizr instance " + 
                (config.getApiUrl() != null ? config.getApiUrl() : "cloud service") + 
                ". Error: " + e.getMessage();
            
            logger.error("Unexpected exception when loading workspace: {}", errorMsg, e);
            throw new RuntimeException(errorMsg, e);
        }
    }
}