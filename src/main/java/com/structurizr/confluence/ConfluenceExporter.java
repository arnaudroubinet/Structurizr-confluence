package com.structurizr.confluence;

import com.atlassian.adf.Document;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.structurizr.Workspace;
import com.structurizr.api.StructurizrClientException;
import com.structurizr.confluence.client.ConfluenceClient;
import com.structurizr.confluence.client.ConfluenceConfig;
import com.structurizr.confluence.client.StructurizrConfig;
import com.structurizr.confluence.client.StructurizrWorkspaceLoader;
import com.structurizr.confluence.processor.AsciidocProcessor;
import com.structurizr.confluence.processor.HtmlToAdfConverter;
import com.structurizr.documentation.Decision;
import com.structurizr.model.*;
import com.structurizr.view.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

/**
 * Exports Structurizr workspace documentation and ADRs to Confluence Cloud in Atlassian Document Format (ADF).
 * Can load workspaces from Structurizr on-premise instances or work with provided workspace objects.
 */
public class ConfluenceExporter {
    
    private static final Logger logger = LoggerFactory.getLogger(ConfluenceExporter.class);
    
    private final ConfluenceClient confluenceClient;
    private final ObjectMapper objectMapper;
    private final StructurizrWorkspaceLoader workspaceLoader;
    private final AsciidocProcessor asciidocProcessor;
    private final HtmlToAdfConverter htmlToAdfConverter;
    
    /**
     * Creates an exporter that loads workspaces from a Structurizr on-premise instance.
     */
    public ConfluenceExporter(ConfluenceConfig confluenceConfig, StructurizrConfig structurizrConfig) throws StructurizrClientException {
        this.confluenceClient = new ConfluenceClient(confluenceConfig);
        this.objectMapper = new ObjectMapper();
        this.workspaceLoader = new StructurizrWorkspaceLoader(structurizrConfig);
        this.asciidocProcessor = new AsciidocProcessor();
        this.htmlToAdfConverter = new HtmlToAdfConverter();
    }
    
    /**
     * Creates an exporter for use with provided workspace objects (original behavior).
     */
    public ConfluenceExporter(ConfluenceConfig confluenceConfig) {
        this.confluenceClient = new ConfluenceClient(confluenceConfig);
        this.objectMapper = new ObjectMapper();
        this.workspaceLoader = null;
        this.asciidocProcessor = new AsciidocProcessor();
        this.htmlToAdfConverter = new HtmlToAdfConverter();
    }
    
    /**
     * Exports a workspace loaded from the configured Structurizr instance.
     * 
     * @throws Exception if export fails
     */
    public void exportFromStructurizr() throws Exception {
        if (workspaceLoader == null) {
            throw new IllegalStateException("No Structurizr configuration provided. Use the constructor with StructurizrConfig or call export(Workspace) directly.");
        }
        
        Workspace workspace = workspaceLoader.loadWorkspace();
        export(workspace);
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
        
        // Export AsciiDoc documentation if available
        exportAsciidocDocumentation(workspace, mainPageId);
        
        // Generate individual view pages
        exportViews(workspace, mainPageId);
        
        // Generate model documentation
        exportModel(workspace, mainPageId);
        
        // Generate ADRs (Architecture Decision Records)
        exportDecisions(workspace, mainPageId);
        
        logger.info("Workspace export completed successfully");
    }
    
    /**
     * Processes and exports AsciiDoc documentation with diagram injection.
     * 
     * @param workspace the workspace context
     * @param parentPageId the parent page ID
     * @throws Exception if processing or export fails
     */
    public void exportAsciidocDocumentation(Workspace workspace, String parentPageId) throws Exception {
        logger.info("Processing AsciiDoc documentation for workspace '{}'", workspace.getName());
        
        try {
            // Process the AsciiDoc resource
            String htmlContent = asciidocProcessor.processAsciidocResource("/financial-risk-system.adoc");
            
            // Extract sections from the processed HTML
            Map<String, String> sections = asciidocProcessor.extractSections(htmlContent);
            
            // Convert sections to ADF and export to Confluence
            for (Map.Entry<String, String> section : sections.entrySet()) {
                String sectionTitle = section.getKey();
                String sectionHtml = section.getValue();
                
                // Convert HTML to ADF
                Document adfDocument = htmlToAdfConverter.convertToAdf(sectionHtml, sectionTitle);
                String adfJson = convertDocumentToJson(adfDocument);
                
                // Create page in Confluence
                String pageTitle = workspace.getName() + " - " + sectionTitle;
                String pageId = confluenceClient.createOrUpdatePage(pageTitle, adfJson, parentPageId);
                
                logger.info("AsciiDoc section '{}' exported to page ID: {}", sectionTitle, pageId);
            }
            
            // Also create a complete documentation page with all sections
            Document completeDoc = htmlToAdfConverter.convertSectionsToAdf(sections, 
                workspace.getName() + " - Complete Documentation");
            String completePageId = confluenceClient.createOrUpdatePage(
                workspace.getName() + " - Complete Documentation", 
                convertDocumentToJson(completeDoc), 
                parentPageId
            );
            
            logger.info("Complete AsciiDoc documentation exported to page ID: {}", completePageId);
            
        } catch (Exception e) {
            logger.error("Error processing AsciiDoc documentation", e);
            // Create fallback documentation page
            createFallbackAsciidocPage(workspace, parentPageId);
        }
    }
    
    /**
     * Creates a fallback documentation page when AsciiDoc processing fails.
     */
    private void createFallbackAsciidocPage(Workspace workspace, String parentPageId) throws Exception {
        logger.warn("Creating fallback AsciiDoc documentation page");
        
        Document fallbackDoc = Document.create()
                .h1(workspace.getName() + " - Documentation (Fallback)")
                .paragraph("AsciiDoc processing encountered an error. This is a fallback documentation page.")
                .h2("Workspace Information")
                .paragraph("Name: " + workspace.getName())
                .paragraph("Description: " + (workspace.getDescription() != null ? workspace.getDescription() : "No description available"))
                .h2("Processing Error")
                .paragraph("The AsciiDoc documentation could not be processed. Please check the logs for more details.");
        
        String pageId = confluenceClient.createOrUpdatePage(
                workspace.getName() + " - Documentation (Fallback)",
                convertDocumentToJson(fallbackDoc),
                parentPageId
        );
        
        logger.info("Fallback documentation page created with ID: {}", pageId);
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
                
                // Add ADRs to table of contents if they exist
                if (workspace.getDocumentation() != null && !workspace.getDocumentation().getDecisions().isEmpty()) {
                    tocList.item("Architecture Decision Records");
                }
            });
        } else {
            doc.bulletList(tocList -> {
                tocList.item("Model Documentation");
                
                // Add ADRs to table of contents if they exist
                if (workspace.getDocumentation() != null && !workspace.getDocumentation().getDecisions().isEmpty()) {
                    tocList.item("Architecture Decision Records");
                }
            });
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
                String viewTitle = view.getTitle();
                if (viewTitle == null || viewTitle.trim().isEmpty()) {
                    viewTitle = view.getKey() != null ? view.getKey() : "Untitled View";
                }
                doc.h4(viewTitle);
                
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
    
    private void exportDecisions(Workspace workspace, String parentPageId) throws Exception {
        if (workspace.getDocumentation() == null || workspace.getDocumentation().getDecisions().isEmpty()) {
            logger.info("No architecture decision records found in workspace");
            return;
        }
        
        Collection<Decision> decisions = workspace.getDocumentation().getDecisions();
        logger.info("Exporting {} architecture decision records", decisions.size());
        
        // Create main ADR page
        Document adrMainDoc = Document.create()
            .h1("Architecture Decision Records (ADRs)")
            .paragraph("This page contains all architecture decision records for this project.");
        
        // Add table of contents for ADRs
        adrMainDoc.h2("Decision Records");
        adrMainDoc.bulletList(list -> {
            for (Decision decision : decisions) {
                String itemText = decision.getId() + ": " + decision.getTitle();
                if (decision.getStatus() != null && !decision.getStatus().trim().isEmpty()) {
                    itemText += " (" + decision.getStatus() + ")";
                }
                list.item(itemText);
            }
        });
        
        String adrMainPageId = confluenceClient.createOrUpdatePage(
            "Architecture Decision Records", 
            convertDocumentToJson(adrMainDoc), 
            parentPageId
        );
        logger.info("Created/updated main ADR page with ID: {}", adrMainPageId);
        
        // Create individual ADR pages
        for (Decision decision : decisions) {
            exportDecision(decision, adrMainPageId);
        }
    }
    
    private void exportDecision(Decision decision, String parentPageId) throws Exception {
        Document decisionDoc = Document.create()
            .h1("ADR " + decision.getId() + ": " + decision.getTitle());
        
        // Add decision metadata
        decisionDoc.h2("Decision Information");
        decisionDoc.bulletList(list -> {
            list.item("ID: " + decision.getId());
            list.item("Title: " + decision.getTitle());
            
            if (decision.getStatus() != null && !decision.getStatus().trim().isEmpty()) {
                list.item("Status: " + decision.getStatus());
            }
            
            if (decision.getDate() != null) {
                list.item("Date: " + decision.getDate().toString());
            }
        });
        
        // Add links to other decisions
        if (!decision.getLinks().isEmpty()) {
            decisionDoc.h2("Related Decisions");
            decisionDoc.bulletList(list -> {
                decision.getLinks().forEach(link -> {
                    String linkText = link.getDescription() + " (ID: " + link.getId() + ")";
                    list.item(linkText);
                });
            });
        }
        
        String pageTitle = "ADR " + decision.getId() + " - " + decision.getTitle();
        confluenceClient.createOrUpdatePage(pageTitle, convertDocumentToJson(decisionDoc), parentPageId);
        logger.info("Created/updated ADR page: {}", pageTitle);
    }
}