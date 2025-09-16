package com.structurizr.confluence;

import com.structurizr.Workspace;
import com.structurizr.confluence.adf.AdfDocument;
import com.structurizr.confluence.adf.AdfNode;
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
    
    public ConfluenceExporter(ConfluenceConfig config) {
        this.confluenceClient = new ConfluenceClient(config);
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
        AdfDocument mainDoc = generateWorkspaceDocumentation(workspace);
        String mainPageId = confluenceClient.createOrUpdatePage(
            workspace.getName() + " - Architecture Documentation", 
            mainDoc
        );
        
        logger.info("Main page created/updated with ID: {}", mainPageId);
        
        // Generate individual view pages
        exportViews(workspace, mainPageId);
        
        // Generate model documentation
        exportModel(workspace, mainPageId);
        
        logger.info("Workspace export completed successfully");
    }
    
    private AdfDocument generateWorkspaceDocumentation(Workspace workspace) {
        AdfDocument doc = new AdfDocument();
        
        // Title
        doc.addContent(AdfNode.heading(1)
            .addContent(AdfNode.text(workspace.getName())));
        
        // Description
        if (workspace.getDescription() != null && !workspace.getDescription().trim().isEmpty()) {
            doc.addContent(AdfNode.paragraph()
                .addContent(AdfNode.text(workspace.getDescription())));
        }
        
        // Table of Contents
        doc.addContent(AdfNode.heading(2)
            .addContent(AdfNode.text("Table of Contents")));
        
        AdfNode tocList = AdfNode.bulletList();
        
        // Add views section
        ViewSet views = workspace.getViews();
        if (hasViews(views)) {
            AdfNode viewsItem = AdfNode.listItem()
                .addContent(AdfNode.paragraph()
                    .addContent(AdfNode.text("Views")));
            
            AdfNode viewsList = AdfNode.bulletList();
            addViewsToToc(views.getSystemLandscapeViews(), viewsList, "System Landscape Views");
            addViewsToToc(views.getSystemContextViews(), viewsList, "System Context Views");
            addViewsToToc(views.getContainerViews(), viewsList, "Container Views");
            addViewsToToc(views.getComponentViews(), viewsList, "Component Views");
            addViewsToToc(views.getDeploymentViews(), viewsList, "Deployment Views");
            
            viewsItem.addContent(viewsList);
            tocList.addContent(viewsItem);
        }
        
        // Add model section
        tocList.addContent(AdfNode.listItem()
            .addContent(AdfNode.paragraph()
                .addContent(AdfNode.text("Model Documentation"))));
        
        doc.addContent(tocList);
        
        // Views Overview
        if (hasViews(views)) {
            doc.addContent(AdfNode.heading(2)
                .addContent(AdfNode.text("Views Overview")));
            
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
    
    private void addViewsToToc(Collection<? extends View> views, AdfNode parentList, String title) {
        if (!views.isEmpty()) {
            AdfNode categoryItem = AdfNode.listItem()
                .addContent(AdfNode.paragraph()
                    .addContent(AdfNode.text(title)));
            
            AdfNode categoryList = AdfNode.bulletList();
            for (View view : views) {
                categoryList.addContent(AdfNode.listItem()
                    .addContent(AdfNode.paragraph()
                        .addContent(AdfNode.text(view.getTitle()))));
            }
            
            categoryItem.addContent(categoryList);
            parentList.addContent(categoryItem);
        }
    }
    
    private void addViewsOverview(AdfDocument doc, Collection<? extends View> views, String title) {
        if (!views.isEmpty()) {
            doc.addContent(AdfNode.heading(3)
                .addContent(AdfNode.text(title)));
            
            for (View view : views) {
                doc.addContent(AdfNode.heading(4)
                    .addContent(AdfNode.text(view.getTitle())));
                
                if (view.getDescription() != null && !view.getDescription().trim().isEmpty()) {
                    doc.addContent(AdfNode.paragraph()
                        .addContent(AdfNode.text(view.getDescription())));
                }
                
                // Add view key information
                doc.addContent(AdfNode.paragraph()
                    .addContent(AdfNode.text("Key: "))
                    .addContent(AdfNode.text(view.getKey()).addMark(com.structurizr.confluence.adf.AdfMark.code())));
            }
        }
    }
    
    private void exportViews(Workspace workspace, String parentPageId) throws IOException {
        ViewSet views = workspace.getViews();
        
        exportViewCategory(views.getSystemLandscapeViews(), "System Landscape Views", parentPageId);
        exportViewCategory(views.getSystemContextViews(), "System Context Views", parentPageId);
        exportViewCategory(views.getContainerViews(), "Container Views", parentPageId);
        exportViewCategory(views.getComponentViews(), "Component Views", parentPageId);
        exportViewCategory(views.getDeploymentViews(), "Deployment Views", parentPageId);
    }
    
    private void exportViewCategory(Collection<? extends View> views, String categoryName, String parentPageId) throws IOException {
        if (views.isEmpty()) {
            return;
        }
        
        // Create category page
        AdfDocument categoryDoc = new AdfDocument();
        categoryDoc.addContent(AdfNode.heading(1)
            .addContent(AdfNode.text(categoryName)));
        
        for (View view : views) {
            categoryDoc.addContent(AdfNode.heading(2)
                .addContent(AdfNode.text(view.getTitle())));
            
            if (view.getDescription() != null && !view.getDescription().trim().isEmpty()) {
                categoryDoc.addContent(AdfNode.paragraph()
                    .addContent(AdfNode.text(view.getDescription())));
            }
            
            // Add view properties
            categoryDoc.addContent(AdfNode.paragraph()
                .addContent(AdfNode.text("Key: "))
                .addContent(AdfNode.text(view.getKey()).addMark(com.structurizr.confluence.adf.AdfMark.code())));
        }
        
        confluenceClient.createOrUpdatePage(categoryName, categoryDoc, parentPageId);
        logger.info("Created/updated page for {}", categoryName);
    }
    
    private void exportModel(Workspace workspace, String parentPageId) throws IOException {
        Model model = workspace.getModel();
        
        AdfDocument modelDoc = new AdfDocument();
        modelDoc.addContent(AdfNode.heading(1)
            .addContent(AdfNode.text("Model Documentation")));
        
        // People
        if (!model.getPeople().isEmpty()) {
            modelDoc.addContent(AdfNode.heading(2)
                .addContent(AdfNode.text("People")));
            
            for (Person person : model.getPeople()) {
                addElementDocumentation(modelDoc, person);
            }
        }
        
        // Software Systems
        if (!model.getSoftwareSystems().isEmpty()) {
            modelDoc.addContent(AdfNode.heading(2)
                .addContent(AdfNode.text("Software Systems")));
            
            for (SoftwareSystem softwareSystem : model.getSoftwareSystems()) {
                addElementDocumentation(modelDoc, softwareSystem);
                
                // Containers
                if (!softwareSystem.getContainers().isEmpty()) {
                    modelDoc.addContent(AdfNode.heading(4)
                        .addContent(AdfNode.text("Containers")));
                    
                    for (Container container : softwareSystem.getContainers()) {
                        addElementDocumentation(modelDoc, container);
                        
                        // Components
                        if (!container.getComponents().isEmpty()) {
                            modelDoc.addContent(AdfNode.heading(5)
                                .addContent(AdfNode.text("Components")));
                            
                            for (Component component : container.getComponents()) {
                                addElementDocumentation(modelDoc, component);
                            }
                        }
                    }
                }
            }
        }
        
        confluenceClient.createOrUpdatePage("Model Documentation", modelDoc, parentPageId);
        logger.info("Created/updated model documentation page");
    }
    
    private void addElementDocumentation(AdfDocument doc, Element element) {
        doc.addContent(AdfNode.heading(3)
            .addContent(AdfNode.text(element.getName())));
        
        if (element.getDescription() != null && !element.getDescription().trim().isEmpty()) {
            doc.addContent(AdfNode.paragraph()
                .addContent(AdfNode.text(element.getDescription())));
        }
        
        // Add element properties
        AdfNode propertiesList = AdfNode.bulletList();
        
        propertiesList.addContent(AdfNode.listItem()
            .addContent(AdfNode.paragraph()
                .addContent(AdfNode.text("ID: "))
                .addContent(AdfNode.text(element.getId()).addMark(com.structurizr.confluence.adf.AdfMark.code()))));
        
        if (element instanceof SoftwareSystem) {
            SoftwareSystem system = (SoftwareSystem) element;
            propertiesList.addContent(AdfNode.listItem()
                .addContent(AdfNode.paragraph()
                    .addContent(AdfNode.text("Location: "))
                    .addContent(AdfNode.text(system.getLocation().toString()))));
        }
        
        if (element instanceof Container) {
            Container container = (Container) element;
            if (container.getTechnology() != null && !container.getTechnology().trim().isEmpty()) {
                propertiesList.addContent(AdfNode.listItem()
                    .addContent(AdfNode.paragraph()
                        .addContent(AdfNode.text("Technology: "))
                        .addContent(AdfNode.text(container.getTechnology()))));
            }
        }
        
        if (element instanceof Component) {
            Component component = (Component) element;
            if (component.getTechnology() != null && !component.getTechnology().trim().isEmpty()) {
                propertiesList.addContent(AdfNode.listItem()
                    .addContent(AdfNode.paragraph()
                        .addContent(AdfNode.text("Technology: "))
                        .addContent(AdfNode.text(component.getTechnology()))));
            }
        }
        
        doc.addContent(propertiesList);
    }
}