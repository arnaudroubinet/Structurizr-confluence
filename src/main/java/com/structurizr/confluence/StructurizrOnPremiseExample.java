package com.structurizr.confluence;

import com.structurizr.confluence.client.ConfluenceConfig;
import com.structurizr.confluence.client.StructurizrConfig;

/**
 * Example showing how to load a workspace from a Structurizr on-premise instance
 * and export it (including ADRs) to Confluence Cloud.
 */
public class StructurizrOnPremiseExample {
    
    public static void main(String[] args) {
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
        
        try {
            // Create exporter that loads from Structurizr on-premise
            ConfluenceExporter exporter = new ConfluenceExporter(confluenceConfig, structurizrConfig);
            
            // Export workspace (including documentation and ADRs) to Confluence
            exporter.exportFromStructurizr();
            
            System.out.println("Workspace (including ADRs) exported successfully from Structurizr on-premise to Confluence!");
            
        } catch (Exception e) {
            System.err.println("Export failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}