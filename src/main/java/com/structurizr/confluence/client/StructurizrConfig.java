package com.structurizr.confluence.client;

/**
 * Configuration for connecting to a Structurizr on-premise instance.
 */
public class StructurizrConfig {
    
    private final String apiUrl;
    private final String apiKey;
    private final String apiSecret;
    private final long workspaceId;
    
    public StructurizrConfig(String apiUrl, String apiKey, String apiSecret, long workspaceId) {
        this.apiUrl = apiUrl;
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
        this.workspaceId = workspaceId;
    }
    
    public String getApiUrl() {
        return apiUrl;
    }
    
    public String getApiKey() {
        return apiKey;
    }
    
    public String getApiSecret() {
        return apiSecret;
    }
    
    public long getWorkspaceId() {
        return workspaceId;
    }
}