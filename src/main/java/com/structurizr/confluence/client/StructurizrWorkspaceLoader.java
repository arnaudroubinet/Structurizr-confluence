package com.structurizr.confluence.client;

import com.structurizr.Workspace;
import com.structurizr.api.StructurizrClient;
import com.structurizr.api.StructurizrClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Client for loading workspaces from a Structurizr on-premise instance.
 */
public class StructurizrWorkspaceLoader {
    
    private static final Logger logger = LoggerFactory.getLogger(StructurizrWorkspaceLoader.class);
    
    private final StructurizrConfig config;
    private final StructurizrClient client;
    
    public StructurizrWorkspaceLoader(StructurizrConfig config) throws StructurizrClientException {
        this.config = config;
        
        // Create StructurizrClient with API URL, key, and secret
        if (config.getApiUrl() != null && !config.getApiUrl().isEmpty()) {
            this.client = new StructurizrClient(config.getApiUrl(), config.getApiKey(), config.getApiSecret());
        } else {
            // Use default Structurizr cloud URL if no API URL provided
            this.client = new StructurizrClient(config.getApiKey(), config.getApiSecret());
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
        
        Workspace workspace = client.getWorkspace(config.getWorkspaceId());
        
        if (workspace == null) {
            throw new RuntimeException("Failed to load workspace " + config.getWorkspaceId());
        }
        
        logger.info("Successfully loaded workspace '{}' (ID: {})", 
            workspace.getName(), config.getWorkspaceId());
        
        return workspace;
    }
}