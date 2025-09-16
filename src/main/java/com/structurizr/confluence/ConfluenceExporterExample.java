package com.structurizr.confluence;

import com.structurizr.Workspace;
import com.structurizr.confluence.client.ConfluenceConfig;
import com.structurizr.model.Model;
import com.structurizr.model.Person;
import com.structurizr.model.SoftwareSystem;
import com.structurizr.view.SystemContextView;
import com.structurizr.view.ViewSet;

/**
 * Example usage of the ConfluenceExporter with a manually created workspace.
 * For loading from Structurizr on-premise instances, see StructurizrOnPremiseExample.
 */
public class ConfluenceExporterExample {
    
    public static void main(String[] args) {
        // Create a simple workspace
        Workspace workspace = new Workspace("Getting Started", "This is a model of my software system.");
        Model model = workspace.getModel();
        
        Person user = model.addPerson("User", "A user of my software system.");
        SoftwareSystem softwareSystem = model.addSoftwareSystem("Software System", "My software system.");
        user.uses(softwareSystem, "Uses");
        
        ViewSet views = workspace.getViews();
        SystemContextView contextView = views.createSystemContextView(softwareSystem, "SystemContext", "An example of a System Context diagram.");
        contextView.addAllSoftwareSystems();
        contextView.addAllPeople();
        
        // Configure Confluence connection
        ConfluenceConfig config = new ConfluenceConfig(
            "https://your-domain.atlassian.net", // Confluence base URL
            "your-email@example.com",            // Your email
            "your-api-token",                    // Your API token
            "SPACE"                              // Space key
        );
        
        // Export to Confluence
        ConfluenceExporter exporter = new ConfluenceExporter(config);
        try {
            exporter.export(workspace);
            System.out.println("Workspace exported successfully to Confluence!");
        } catch (Exception e) {
            System.err.println("Export failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}