package com.structurizr.confluence;

import com.atlassian.adf.Document;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.structurizr.Workspace;
import com.structurizr.confluence.client.ConfluenceClient;
import com.structurizr.confluence.client.ConfluenceConfig;
import com.structurizr.model.*;
import com.structurizr.view.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collection;

/**
 * Exports Structurizr workspace documentation to Confluence Cloud in Atlassian Document Format (ADF).
 */
public class ConfluenceExporter {
    
    private static final Logger logger = LoggerFactory.getLogger(ConfluenceExporter.class);
    
    private final ConfluenceClient confluenceClient;
    private final ObjectMapper objectMapper;
    
    public ConfluenceExporter(ConfluenceConfig config) {
        this.confluenceClient = new ConfluenceClient(config);
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * Exports the given workspace to Confluence Cloud.
     * 
     * @param workspace the workspace to export
     * @throws Exception if export fails
     */
    public void export(Workspace workspace) throws Exception {
        logger.info("Starting export of workspace '{}' to Confluence", workspace.getName());
        
        // Generate main documentation page
        Document mainDoc = generateWorkspaceDocumentation(workspace);
        String mainPageId = confluenceClient.createOrUpdatePage(
            workspace.getName() + " - Architecture Documentation", 
            convertDocumentToJson(mainDoc)
        );
        
        logger.info("Main page created/updated with ID: {}", mainPageId);
        
        // Generate individual view pages
        exportViews(workspace, mainPageId);
        
        // Generate model documentation
        exportModel(workspace, mainPageId);
        
        logger.info("Workspace export completed successfully");
    }
    
    private String convertDocumentToJson(Document document) throws Exception {
        return objectMapper.writeValueAsString(document);
    }
    
    private Document generateWorkspaceDocumentation(Workspace workspace) {
        Document doc = Document.create()
            .h1(workspace.getName());
        
        // Description
        if (workspace.getDescription() != null && !workspace.getDescription().trim().isEmpty()) {
            doc.paragraph(workspace.getDescription());
        }
        
        // Table of Contents
        doc.h2("Table of Contents");
        
        // Add views section
        ViewSet views = workspace.getViews();
        if (hasViews(views)) {
            doc.bulletList(tocList -> {
                tocList.item("Views");
                
                if (!views.getSystemLandscapeViews().isEmpty()) {
                    tocList.item("System Landscape Views");
                }
                if (!views.getSystemContextViews().isEmpty()) {
                    tocList.item("System Context Views");
                }
                if (!views.getContainerViews().isEmpty()) {
                    tocList.item("Container Views");
                }
                if (!views.getComponentViews().isEmpty()) {
                    tocList.item("Component Views");
                }
                if (!views.getDeploymentViews().isEmpty()) {
                    tocList.item("Deployment Views");
                }
                
                tocList.item("Model Documentation");
            });
        } else {
            doc.bulletList(tocList -> tocList.item("Model Documentation"));
        }
        
        // Views Overview
        if (hasViews(views)) {
            doc.h2("Views Overview");
            
            addViewsOverview(doc, views.getSystemLandscapeViews(), "System Landscape Views");
            addViewsOverview(doc, views.getSystemContextViews(), "System Context Views");
            addViewsOverview(doc, views.getContainerViews(), "Container Views");
            addViewsOverview(doc, views.getComponentViews(), "Component Views");
            addViewsOverview(doc, views.getDeploymentViews(), "Deployment Views");
        }
        
        return doc;
    }
    
    private boolean hasViews(ViewSet views) {
        return (views.getSystemLandscapeViews().size() > 0 ||
                views.getSystemContextViews().size() > 0 ||
                views.getContainerViews().size() > 0 ||
                views.getComponentViews().size() > 0 ||
                views.getDeploymentViews().size() > 0);
    }
    
    private void addViewsOverview(Document doc, Collection<? extends View> views, String title) {
        if (!views.isEmpty()) {
            doc.h3(title);
            
            for (View view : views) {
                doc.h4(view.getTitle());
                
                if (view.getDescription() != null && !view.getDescription().trim().isEmpty()) {
                    doc.paragraph(view.getDescription());
                }
                
                // Add view key information
                doc.paragraph("Key: " + view.getKey());
            }
        }
    }
    
    private void exportViews(Workspace workspace, String parentPageId) throws Exception {
        ViewSet views = workspace.getViews();
        
        exportViewCategory(views.getSystemLandscapeViews(), "System Landscape Views", parentPageId);
        exportViewCategory(views.getSystemContextViews(), "System Context Views", parentPageId);
        exportViewCategory(views.getContainerViews(), "Container Views", parentPageId);
        exportViewCategory(views.getComponentViews(), "Component Views", parentPageId);
        exportViewCategory(views.getDeploymentViews(), "Deployment Views", parentPageId);
    }
    
    private void exportViewCategory(Collection<? extends View> views, String categoryName, String parentPageId) throws Exception {
        if (views.isEmpty()) {
            return;
        }
        
        // Create category page
        Document categoryDoc = Document.create()
            .h1(categoryName);
        
        for (View view : views) {
            categoryDoc.h2(view.getTitle());
            
            if (view.getDescription() != null && !view.getDescription().trim().isEmpty()) {
                categoryDoc.paragraph(view.getDescription());
            }
            
            // Add view properties
            categoryDoc.paragraph("Key: " + view.getKey());
        }
        
        confluenceClient.createOrUpdatePage(categoryName, convertDocumentToJson(categoryDoc), parentPageId);
        logger.info("Created/updated page for {}", categoryName);
    }
    
    private void exportModel(Workspace workspace, String parentPageId) throws Exception {
        Model model = workspace.getModel();
        
        Document modelDoc = Document.create()
            .h1("Model Documentation");
        
        // People
        if (!model.getPeople().isEmpty()) {
            modelDoc.h2("People");
            
            for (Person person : model.getPeople()) {
                addElementDocumentation(modelDoc, person);
            }
        }
        
        // Software Systems
        if (!model.getSoftwareSystems().isEmpty()) {
            modelDoc.h2("Software Systems");
            
            for (SoftwareSystem softwareSystem : model.getSoftwareSystems()) {
                addElementDocumentation(modelDoc, softwareSystem);
                
                // Containers
                if (!softwareSystem.getContainers().isEmpty()) {
                    modelDoc.h4("Containers");
                    
                    for (Container container : softwareSystem.getContainers()) {
                        addElementDocumentation(modelDoc, container);
                        
                        // Components
                        if (!container.getComponents().isEmpty()) {
                            modelDoc.h5("Components");
                            
                            for (Component component : container.getComponents()) {
                                addElementDocumentation(modelDoc, component);
                            }
                        }
                    }
                }
            }
        }
        
        confluenceClient.createOrUpdatePage("Model Documentation", convertDocumentToJson(modelDoc), parentPageId);
        logger.info("Created/updated model documentation page");
    }
    
    private void addElementDocumentation(Document doc, Element element) {
        doc.h3(element.getName());
        
        if (element.getDescription() != null && !element.getDescription().trim().isEmpty()) {
            doc.paragraph(element.getDescription());
        }
        
        // Add element properties
        doc.bulletList(list -> {
            list.item("ID: " + element.getId());
            
            if (element instanceof SoftwareSystem) {
                SoftwareSystem system = (SoftwareSystem) element;
                list.item("Location: " + system.getLocation().toString());
            }
            
            if (element instanceof Container) {
                Container container = (Container) element;
                if (container.getTechnology() != null && !container.getTechnology().trim().isEmpty()) {
                    list.item("Technology: " + container.getTechnology());
                }
            }
            
            if (element instanceof Component) {
                Component component = (Component) element;
                if (component.getTechnology() != null && !component.getTechnology().trim().isEmpty()) {
                    list.item("Technology: " + component.getTechnology());
                }
            }
        });
    }
}