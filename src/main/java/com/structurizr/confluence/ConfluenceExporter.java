package com.structurizr.confluence;

import com.atlassian.adf.Document;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.structurizr.Workspace;
import com.structurizr.api.StructurizrClientException;
import com.structurizr.confluence.client.ConfluenceClient;
import com.structurizr.confluence.client.ConfluenceConfig;
import com.structurizr.confluence.client.StructurizrConfig;
import com.structurizr.confluence.client.StructurizrWorkspaceLoader;
import com.structurizr.confluence.processor.AsciiDocConverter;
import com.structurizr.confluence.processor.HtmlToAdfConverter;
import com.structurizr.documentation.Decision;
import com.structurizr.model.*;
import com.structurizr.view.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

/**
 * Exports Structurizr workspace documentation and ADRs to Confluence Cloud in Atlassian Document Format (ADF).
 * Can load workspaces from Structurizr on-premise instances or work with provided workspace objects.
 */
public class ConfluenceExporter {
    
    private static final Logger logger = LoggerFactory.getLogger(ConfluenceExporter.class);
    
    private final ConfluenceClient confluenceClient;
    private final ObjectMapper objectMapper;
    private final StructurizrWorkspaceLoader workspaceLoader;
    private final HtmlToAdfConverter htmlToAdfConverter;
    private final AsciiDocConverter asciiDocConverter;
    
    /**
     * Creates an exporter that loads workspaces from a Structurizr on-premise instance.
     */
    public ConfluenceExporter(ConfluenceConfig confluenceConfig, StructurizrConfig structurizrConfig) throws StructurizrClientException {
        this.confluenceClient = new ConfluenceClient(confluenceConfig);
        this.objectMapper = new ObjectMapper();
        this.workspaceLoader = new StructurizrWorkspaceLoader(structurizrConfig);
        this.htmlToAdfConverter = new HtmlToAdfConverter();
        this.asciiDocConverter = new AsciiDocConverter();
    }
    
    /**
     * Creates an exporter for use with provided workspace objects (original behavior).
     */
    public ConfluenceExporter(ConfluenceConfig confluenceConfig) {
        this.confluenceClient = new ConfluenceClient(confluenceConfig);
        this.objectMapper = new ObjectMapper();
        this.workspaceLoader = null;
        this.htmlToAdfConverter = new HtmlToAdfConverter();
        this.asciiDocConverter = new AsciiDocConverter();
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

    public void export(Workspace workspace, String branchName) throws Exception {
        logger.info("Starting export of workspace '{}' (branch '{}') to Confluence", workspace.getName(), branchName);

        // Générer la page principale avec le nom de la branche
        String mainPageTitle = branchName;
        Document mainDoc = generateWorkspaceDocumentation(workspace, branchName);
        String mainPageId = confluenceClient.createOrUpdatePage(
            mainPageTitle,
            convertDocumentToJson(mainDoc)
        );

        logger.info("Main page created/updated with ID: {}", mainPageId);

        // Créer la page Documentation sous la page principale
        String documentationPageTitle = "Documentation";
        Document documentationDoc = Document.create()
            .h1("Documentation")
            .paragraph("Cette page contient la documentation du workspace. Voir les sous-pages pour chaque section.")
            .h2("Sommaire");

        // Générer la liste des sections de documentation pour le sommaire
        if (workspace.getDocumentation() != null && !workspace.getDocumentation().getSections().isEmpty()) {
            documentationDoc.bulletList(list -> {
                for (com.structurizr.documentation.Section section : workspace.getDocumentation().getSections()) {
                    String sectionTitle = section.getTitle() != null && !section.getTitle().isEmpty() ? section.getTitle() : section.getFilename();
                    String pageTitle = branchName + " - " + sectionTitle;
                    list.item(pageTitle);
                }
            });
        } else {
            documentationDoc.paragraph("Aucune section de documentation trouvée dans le workspace.");
        }

        String documentationPageId = confluenceClient.createOrUpdatePage(
            documentationPageTitle,
            convertDocumentToJson(documentationDoc),
            mainPageId
        );
        logger.info("Documentation page created/updated with ID: {}", documentationPageId);

        // Exporter la documentation du workspace (sections) sous la page Documentation
        exportWorkspaceDocumentationSections(workspace, documentationPageId, branchName);

    // Générer les pages de vues
    exportViews(workspace, mainPageId);

    // Générer la documentation du modèle
    exportModel(workspace, mainPageId);

    // Générer les ADRs
    exportDecisions(workspace, mainPageId);

    // Générer la page Schémas
    exportDiagramsPage(mainPageId, workspace);

    logger.info("Workspace export completed successfully");
    }

    /**
     * Closes resources used by the exporter.
     */
    public void close() {
        if (asciiDocConverter != null) {
            asciiDocConverter.close();
        }
    }
    
    /**
     * Cleans the Confluence space by deleting all existing pages.
     * 
     * @throws Exception if cleanup fails
     */
    public void cleanConfluenceSpace() throws Exception {
        logger.info("Starting Confluence space cleanup");
        confluenceClient.cleanSpace();
        logger.info("Confluence space cleanup completed");
    }

    // Pour compatibilité ascendante
    public void export(Workspace workspace) throws Exception {
        export(workspace, workspace.getName());
    }
    
    /**
     * Processes and exports AsciiDoc documentation with diagram injection.
     * 
     * @param workspace the workspace context
     * @param parentPageId the parent page ID
     * @throws Exception if processing or export fails
     */
    /**
     * Exporte la documentation présente dans le Workspace (sections AsciiDoc déjà importées).
     * @param workspace le workspace Structurizr
     * @param parentPageId l'ID de la page parente Confluence
     */
    public void exportWorkspaceDocumentationSections(Workspace workspace, String parentPageId, String branchName) throws Exception {
        if (workspace.getDocumentation() == null || workspace.getDocumentation().getSections().isEmpty()) {
            logger.info("Aucune section de documentation trouvée dans le workspace");
            return;
        }

        logger.info("Export des sections de documentation du workspace '{}', {} section(s)", workspace.getName(), workspace.getDocumentation().getSections().size());

        // Exporter chaque section comme page Confluence
        for (com.structurizr.documentation.Section section : workspace.getDocumentation().getSections()) {
            String sectionTitle = section.getTitle() != null && !section.getTitle().isEmpty() ? section.getTitle() : section.getFilename();
            String content = section.getContent();

            // Déterminer le format et convertir si nécessaire
            String htmlContent;
            String formatName = section.getFormat() != null ? section.getFormat().name() : "";
            
            if ("AsciiDoc".equalsIgnoreCase(formatName) || "asciidoc".equalsIgnoreCase(formatName)) {
                logger.debug("Converting AsciiDoc content for section: {}", sectionTitle);
                htmlContent = asciiDocConverter.convertToHtml(content, sectionTitle);
            } else if ("Markdown".equalsIgnoreCase(formatName) || "md".equalsIgnoreCase(formatName)) {
                logger.debug("Markdown content detected for section: {} (treating as HTML)", sectionTitle);
                // Pour Markdown, on pourrait ajouter un convertisseur Markdown->HTML plus tard
                htmlContent = content; // Traitement basique pour l'instant
            } else {
                logger.debug("Treating content as HTML for section: {} (format: {})", sectionTitle, formatName);
                htmlContent = content; // Assume HTML ou texte brut
            }

            // Convertir HTML vers ADF pour Confluence
            Document adfDocument = htmlToAdfConverter.convertToAdf(htmlContent, sectionTitle);
            String adfJson = convertDocumentToJson(adfDocument);

            String pageTitle = branchName + " - " + sectionTitle;
            String pageId = confluenceClient.createOrUpdatePage(pageTitle, adfJson, parentPageId);
            logger.info("Section '{}' exportée vers la page ID: {}", sectionTitle, pageId);
        }
    }
    
    


    private String convertDocumentToJson(Document document) throws Exception {
        return objectMapper.writeValueAsString(document);
    }


    
    private Document generateWorkspaceDocumentation(Workspace workspace, String branchName) {
        Document doc = Document.create()
            .h1(branchName);

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
        
        // Add decision content (convert from AsciiDoc/Markdown if needed)
        if (decision.getContent() != null && !decision.getContent().trim().isEmpty()) {
            decisionDoc.h2("Content");
            
            String formatName = decision.getFormat() != null ? decision.getFormat().name() : "";
            String htmlContent;
            
            if ("AsciiDoc".equalsIgnoreCase(formatName) || "asciidoc".equalsIgnoreCase(formatName)) {
                logger.debug("Converting AsciiDoc content for ADR: {}", decision.getTitle());
                htmlContent = asciiDocConverter.convertToHtml(decision.getContent(), "ADR " + decision.getId());
            } else if ("Markdown".equalsIgnoreCase(formatName) || "md".equalsIgnoreCase(formatName)) {
                logger.debug("Markdown content detected for ADR: {} (treating as HTML)", decision.getTitle());
                htmlContent = decision.getContent(); // Traitement basique pour l'instant
            } else {
                logger.debug("Treating content as HTML for ADR: {} (format: {})", decision.getTitle(), formatName);
                htmlContent = decision.getContent(); // Assume HTML ou texte brut
            }
            
            // Add the converted content to the document
            decisionDoc.paragraph(htmlContent);
        }
        
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
    
    /**
     * Crée une page "Schémas" sous la page principale, listant dynamiquement les PNG Structurizr pour chaque vue.
     */
    private void exportDiagramsPage(String parentPageId, Workspace workspace) throws Exception {
        String structurizrServerUrl = System.getenv("STRUCTURIZR_SERVER_URL");
        String workspaceId = System.getenv("STRUCTURIZR_WORKSPACE_ID");
        if (structurizrServerUrl == null) structurizrServerUrl = "https://static.structurizr.com";
        if (workspaceId == null) workspaceId = "1";

        Document diagramsDoc = Document.create()
            .h1("Schémas")
            .paragraph("Cette page regroupe tous les schémas PNG du workspace Structurizr (serveur: " + structurizrServerUrl + ", workspace: " + workspaceId + ").");

        if (workspace == null) {
            diagramsDoc.paragraph("Impossible d'accéder au workspace pour lister les vues.");
        } else {
            ViewSet views = workspace.getViews();
            for (View view : views.getViews()) {
                String diagramKey = view.getKey();
                String diagramName = view.getTitle() != null && !view.getTitle().isEmpty() ? view.getTitle() : diagramKey;
                String pngUrl = structurizrServerUrl + "/workspace/" + workspaceId + "/diagrams/" + diagramKey + ".png";
                // Vérifier si l'image est accessible
                boolean accessible = false;
                try {
                    java.net.HttpURLConnection connection = (java.net.HttpURLConnection) new java.net.URL(pngUrl).openConnection();
                    connection.setRequestMethod("HEAD");
                    connection.setConnectTimeout(2000);
                    connection.setReadTimeout(2000);
                    int responseCode = connection.getResponseCode();
                    accessible = (responseCode >= 200 && responseCode < 300);
                } catch (Exception e) {
                    accessible = false;
                }
                diagramsDoc.h2(diagramName);
                if (accessible) {
                    diagramsDoc.paragraph("![" + diagramName + "](" + pngUrl + ")");
                } else {
                    diagramsDoc.paragraph("Erreur de récupération du schéma : " + pngUrl);
                }
            }
        }

        confluenceClient.createOrUpdatePage("Schémas", convertDocumentToJson(diagramsDoc), parentPageId);
        logger.info("Page 'Schémas' créée/mise à jour sous la page principale (PNG Structurizr).");
    }
}