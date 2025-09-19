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
import com.structurizr.confluence.processor.ImageUploadManager;
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
    exportDecisions(workspace, mainPageId, branchName);

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
                String workspaceId = getWorkspaceId(workspace);
                htmlContent = asciiDocConverter.convertToHtml(content, sectionTitle, workspaceId, branchName);
            } else if ("Markdown".equalsIgnoreCase(formatName) || "md".equalsIgnoreCase(formatName)) {
                logger.debug("Markdown content detected for section: {} (treating as HTML)", sectionTitle);
                // Pour Markdown, on pourrait ajouter un convertisseur Markdown->HTML plus tard
                htmlContent = content; // Traitement basique pour l'instant
            } else {
                logger.debug("Treating content as HTML for section: {} (format: {})", sectionTitle, formatName);
                htmlContent = content; // Assume HTML ou texte brut
            }

            // Extraire le titre du contenu HTML (premier H1) si disponible
            String extractedTitle = htmlToAdfConverter.extractPageTitleOnly(htmlContent);
            String actualTitle = extractedTitle != null && !extractedTitle.trim().isEmpty() ? extractedTitle : sectionTitle;
            
            // Setup image upload manager for this page
            ImageUploadManager imageUploadManager = new ImageUploadManager(confluenceClient);
            htmlToAdfConverter.setImageUploadManager(imageUploadManager);
            
            // Create page first to get the page ID for image uploads
            String pageTitle = actualTitle;
            String pageId = confluenceClient.createOrUpdatePage(pageTitle, "{\"version\":1,\"type\":\"doc\",\"content\":[]}", parentPageId);
            
            // Set page context for image uploads
            htmlToAdfConverter.setCurrentPageId(pageId);
            
            // Convertir HTML vers ADF JSON pour Confluence avec support des tables natives
            String adfJson = htmlToAdfConverter.convertToAdfJson(htmlContent, actualTitle);

            // Update page with actual content
            confluenceClient.updatePageById(pageId, pageTitle, adfJson);
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
    
    private void exportDecisions(Workspace workspace, String parentPageId, String branchName) throws Exception {
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
            exportDecision(decision, adrMainPageId, workspace, branchName);
        }
    }
    
    private void exportDecision(Decision decision, String parentPageId, Workspace workspace, String branchName) throws Exception {
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
                String workspaceId = getWorkspaceId(workspace);
                htmlContent = asciiDocConverter.convertToHtml(decision.getContent(), "ADR " + decision.getId(), workspaceId, branchName);
            } else if ("Markdown".equalsIgnoreCase(formatName) || "md".equalsIgnoreCase(formatName)) {
                logger.debug("Converting Markdown content for ADR: {}", decision.getTitle());
                htmlContent = convertBasicMarkdownToHtml(decision.getContent());
            } else {
                logger.debug("Treating content as HTML for ADR: {} (format: {})", decision.getTitle(), formatName);
                htmlContent = decision.getContent(); // Assume HTML ou texte brut
            }
            
            // Convert HTML content to structured ADF instead of plain text
            Document convertedContent = htmlToAdfConverter.convertToAdf(htmlContent, "Content");
            decisionDoc = combineDocuments(decisionDoc, convertedContent);
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
     * Generates a Confluence page with workspace diagrams.
     * For workspaces loaded from Structurizr on-premise, attempts to link to actual diagram images.
     * For workspaces loaded from JSON files, shows diagram information only.
     */
    private void exportDiagramsPage(String parentPageId, Workspace workspace) throws Exception {
        String workspaceId = getWorkspaceId(workspace);
        boolean isFromStructurizr = workspaceLoader != null; // Indicates workspace from Structurizr instance
        
        Document diagramsDoc = Document.create()
            .h1("Schémas");
            
        if (isFromStructurizr) {
            String structurizrServerUrl = System.getenv("STRUCTURIZR_SERVER_URL");
            if (structurizrServerUrl == null) structurizrServerUrl = "https://structurizr.roubinet.fr";
            diagramsDoc.paragraph("Cette page regroupe tous les schémas PNG du workspace Structurizr (serveur: " + structurizrServerUrl + ", workspace: " + workspaceId + ").");
        } else {
            diagramsDoc.paragraph("Cette page liste les vues/diagrammes disponibles dans le workspace (chargé depuis un fichier JSON).");
        }

        if (workspace == null) {
            diagramsDoc.paragraph("Impossible d'accéder au workspace pour lister les vues.");
        } else {
            ViewSet views = workspace.getViews();
            if (views == null || views.getViews().isEmpty()) {
                diagramsDoc.paragraph("Aucune vue trouvée dans le workspace.");
            } else {
                for (View view : views.getViews()) {
                    String diagramKey = view.getKey();
                    String diagramName = view.getTitle() != null && !view.getTitle().isEmpty() ? view.getTitle() : diagramKey;
                    
                    diagramsDoc.h2(diagramName);
                    diagramsDoc.bulletList(list -> {
                        list.item("Clé : " + diagramKey);
                        list.item("Type : " + view.getClass().getSimpleName());
                        if (view.getDescription() != null && !view.getDescription().trim().isEmpty()) {
                            list.item("Description : " + view.getDescription());
                        }
                    });
                    
                    if (isFromStructurizr) {
                        // Try to link to actual diagram images for Structurizr workspaces
                        String structurizrServerUrl = System.getenv("STRUCTURIZR_SERVER_URL");
                        if (structurizrServerUrl == null) structurizrServerUrl = "https://structurizr.roubinet.fr";
                        String pngUrl = structurizrServerUrl + "/workspace/" + workspaceId + "/diagrams/" + diagramKey + ".png";
                        
                        // Test if image is accessible
                        boolean accessible = false;
                        try {
                            java.net.URI uri = java.net.URI.create(pngUrl);
                            java.net.HttpURLConnection connection = (java.net.HttpURLConnection) uri.toURL().openConnection();
                            connection.setRequestMethod("HEAD");
                            connection.setConnectTimeout(2000);
                            connection.setReadTimeout(2000);
                            int responseCode = connection.getResponseCode();
                            accessible = (responseCode >= 200 && responseCode < 300);
                        } catch (Exception e) {
                            logger.debug("Could not access diagram image at: {}", pngUrl, e);
                            accessible = false;
                        }
                        
                        if (accessible) {
                            diagramsDoc.paragraph("![" + diagramName + "](" + pngUrl + ")");
                        } else {
                            diagramsDoc.paragraph("⚠️ Image non accessible : " + pngUrl);
                        }
                    } else {
                        // For JSON workspaces, provide placeholder
                        diagramsDoc.paragraph("📊 Diagramme disponible - pour voir l'image, générez-la avec les outils Structurizr.");
                    }
                }
            }
        }

        confluenceClient.createOrUpdatePage("Schémas", convertDocumentToJson(diagramsDoc), parentPageId);
        logger.info("Page 'Schémas' créée/mise à jour sous la page principale.");
    }

    /**
     * Extracts workspace ID from workspace, using ID property or falling back to a default.
     * 
     * @param workspace the workspace
     * @return workspace ID as string
     */
    private String getWorkspaceId(Workspace workspace) {
        // Get ID from workspace - it's a long, so convert to string
        long workspaceId = workspace.getId();
        return String.valueOf(workspaceId);
    }
    
    /**
     * Converts basic Markdown syntax to HTML.
     * Handles headings, paragraphs, links, and basic formatting.
     */
    private String convertBasicMarkdownToHtml(String markdown) {
        if (markdown == null || markdown.trim().isEmpty()) {
            return "";
        }
        
        String html = markdown;
        
        // Convert headings
        html = html.replaceAll("(?m)^# (.+)$", "<h1>$1</h1>");
        html = html.replaceAll("(?m)^## (.+)$", "<h2>$1</h2>");
        html = html.replaceAll("(?m)^### (.+)$", "<h3>$1</h3>");
        html = html.replaceAll("(?m)^#### (.+)$", "<h4>$1</h4>");
        html = html.replaceAll("(?m)^##### (.+)$", "<h5>$1</h5>");
        html = html.replaceAll("(?m)^###### (.+)$", "<h6>$1</h6>");
        
        // Convert links: [text](url) -> <a href="url">text</a>
        html = html.replaceAll("\\[([^\\]]+)\\]\\(([^\\)]+)\\)", "<a href=\"$2\">$1</a>");
        
        // Convert simple URL links: http://... -> <a href="...">...</a>
        html = html.replaceAll("(https?://[^\\s]+)", "<a href=\"$1\">$1</a>");
        
        // Convert bold: **text** -> <strong>text</strong>
        html = html.replaceAll("\\*\\*([^\\*]+)\\*\\*", "<strong>$1</strong>");
        
        // Convert italic: *text* -> <em>text</em>
        html = html.replaceAll("(?<!\\*)\\*([^\\*]+)\\*(?!\\*)", "<em>$1</em>");
        
        // Convert line breaks to paragraphs
        // Split by double line breaks (paragraph separators)
        String[] paragraphs = html.split("\\n\\s*\\n");
        StringBuilder result = new StringBuilder();
        
        for (String paragraph : paragraphs) {
            String trimmed = paragraph.trim();
            if (!trimmed.isEmpty()) {
                // Skip if already has HTML tags
                if (!trimmed.matches(".*<h[1-6]>.*</h[1-6]>.*")) {
                    result.append("<p>").append(trimmed.replace("\n", " ")).append("</p>\n");
                } else {
                    result.append(trimmed).append("\n");
                }
            }
        }
        
        return result.toString();
    }
    
    /**
     * Combines two ADF documents by merging their content.
     */
    private Document combineDocuments(Document base, Document addition) {
        // This is a simplified approach - in practice you'd need proper ADF manipulation
        // For now, return the addition since it contains the converted content
        return addition;
    }
}