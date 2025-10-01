package arnaudroubinet.structurizr.confluence;

import com.atlassian.adf.Document;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.structurizr.Workspace;
import com.structurizr.api.StructurizrClientException;
import arnaudroubinet.structurizr.confluence.client.ConfluenceClient;
import arnaudroubinet.structurizr.confluence.client.ConfluenceConfig;
import arnaudroubinet.structurizr.confluence.client.StructurizrConfig;
import arnaudroubinet.structurizr.confluence.client.StructurizrWorkspaceLoader;
import arnaudroubinet.structurizr.confluence.processor.AsciiDocConverter;
import arnaudroubinet.structurizr.confluence.processor.HtmlToAdfConverter;
import arnaudroubinet.structurizr.confluence.processor.ImageUploadManager;
import arnaudroubinet.structurizr.confluence.processor.DiagramExporter;
import arnaudroubinet.structurizr.confluence.processor.MarkdownConverter;
import com.structurizr.documentation.Decision;
import com.structurizr.model.*;
import com.structurizr.view.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

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
    private final MarkdownConverter markdownConverter;
    private List<File> exportedDiagrams;
    
    /**
     * Creates an exporter that loads workspaces from a Structurizr on-premise instance.
     */
    public ConfluenceExporter(ConfluenceConfig confluenceConfig, StructurizrConfig structurizrConfig) throws StructurizrClientException {
        this.confluenceClient = new ConfluenceClient(confluenceConfig);
        this.objectMapper = new ObjectMapper();
        this.workspaceLoader = new StructurizrWorkspaceLoader(structurizrConfig);
        this.htmlToAdfConverter = new HtmlToAdfConverter();
    this.asciiDocConverter = new AsciiDocConverter();
    this.markdownConverter = new MarkdownConverter();
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
    this.markdownConverter = new MarkdownConverter();
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
     * Exports a workspace loaded from the configured Structurizr instance to a specific parent page.
     * 
     * @param parentPageId the parent page ID
     * @param branchName the branch name
     * @throws Exception if export fails
     */
    public void exportFromStructurizr(String parentPageId, String branchName) throws Exception {
        if (workspaceLoader == null) {
            throw new IllegalStateException("No Structurizr configuration provided. Use the constructor with StructurizrConfig or call export(Workspace) directly.");
        }
        
        Workspace workspace = workspaceLoader.loadWorkspace();
        export(workspace, parentPageId, branchName);
    }
    
    /**
     * Exports the given workspace to Confluence Cloud.
     * 
     * @param workspace the workspace to export
     * @throws Exception if export fails
     */

    public void export(Workspace workspace, String branchName) throws Exception {
        logger.info("Starting export of workspace '{}' (branch '{}') to Confluence", workspace.getName(), branchName);

        String workspaceId = getWorkspaceId(workspace);
        DiagramExporter diagramExporter = DiagramExporter.fromEnvironment(workspaceId);
        List<File> exportedDiagrams = null;

        if (diagramExporter == null) {
            throw new IllegalStateException("Diagram export via Puppeteer is required but environment variables are not configured. Please define STRUCTURIZR_URL, STRUCTURIZR_USERNAME and STRUCTURIZR_PASSWORD.");
        }

        try {
            logger.info("Exporting diagrams using Playwright...");
            exportedDiagrams = diagramExporter.exportDiagrams(workspace);
            logger.info("Successfully exported {} diagrams", exportedDiagrams.size());
        } catch (Exception e) {
            logger.warn("Diagram export failed, continuing without diagrams: {}", e.getMessage());
            // Don't fail the entire process if diagram export fails
            // throw new IllegalStateException("Diagram export via Playwright failed. Stopping process.", e);
        }

        this.exportedDiagrams = exportedDiagrams;
        
        if (exportedDiagrams != null) {
            Function<String, File> diagramResolver = this::getDiagramFile;
            asciiDocConverter.setDiagramResolver(diagramResolver);
            htmlToAdfConverter.setDiagramResolver(diagramResolver);
            logger.info("Configured converters to use {} local diagram files", exportedDiagrams.size());
        }

        String mainPageTitle = branchName;
        Document mainDoc = generateWorkspaceDocumentation(workspace, branchName);
        String mainPageId = confluenceClient.createOrUpdatePage(
            mainPageTitle,
            convertDocumentToJson(mainDoc)
        );

        logger.info("Main page created/updated with ID: {}", mainPageId);

        String documentationPageTitle = "Documentation";
        String documentationPageId = confluenceClient.createOrUpdatePage(
            documentationPageTitle,
            "{\"version\":1,\"type\":\"doc\",\"content\":[]}",
            mainPageId
        );
        logger.info("Documentation page created/updated with ID: {}", documentationPageId);

        // Configurer l’upload d’images pour la page Documentation
        ImageUploadManager docImageUploadManager = new ImageUploadManager(confluenceClient);
        htmlToAdfConverter.setImageUploadManager(docImageUploadManager);
        htmlToAdfConverter.setCurrentPageId(documentationPageId);

        Document documentationDoc = Document.create();

        String documentationJson = convertDocumentToJson(documentationDoc);
        ObjectNode documentationNode = objectMapper.readTree(documentationJson) instanceof ObjectNode
            ? (ObjectNode) objectMapper.readTree(documentationJson)
            : objectMapper.createObjectNode();
        ArrayNode docContent = documentationNode.has("content") && documentationNode.get("content").isArray()
            ? (ArrayNode) documentationNode.get("content")
            : documentationNode.putArray("content");

        // Macro TOC Confluence (table of contents)
        ObjectNode tocNode = objectMapper.createObjectNode();
        tocNode.put("type", "extension");
        ObjectNode extAttrs = objectMapper.createObjectNode();
        // ADF macro format requires extensionType + extensionKey (macro name)
        // https://developer.atlassian.com/platform/forge/adopting-forge-from-connect-migrate-macro
        extAttrs.put("extensionType", "com.atlassian.confluence.macro.core");
        extAttrs.put("extensionKey", "toc");
        // Default options (let Confluence manage), no parameters required
        tocNode.set("attrs", extAttrs);
        docContent.add(tocNode);

        if (workspace.getDocumentation() != null && !workspace.getDocumentation().getSections().isEmpty()) {
            for (com.structurizr.documentation.Section section : workspace.getDocumentation().getSections()) {
                String filenameFallback = section.getFilename();
                String content = section.getContent();

                String htmlContent;
                String formatName = section.getFormat() != null ? section.getFormat().name() : "";
                if ("AsciiDoc".equalsIgnoreCase(formatName) || "asciidoc".equalsIgnoreCase(formatName)) {
                    String workspaceId2 = getWorkspaceId(workspace);
                    htmlContent = asciiDocConverter.convertToHtml(content, filenameFallback, workspaceId2, branchName);
                } else if ("Markdown".equalsIgnoreCase(formatName) || "md".equalsIgnoreCase(formatName)) {
                    htmlContent = markdownConverter.toHtml(content);
                } else {
                    htmlContent = content;
                }

                htmlToAdfConverter.extractPageTitleOnly(htmlContent);

                // Convert section HTML to ADF JSON (avec post-traitements)
                String sectionAdfJson = htmlToAdfConverter.convertToAdfJson(htmlContent, filenameFallback);
                ObjectNode sectionDocNode = objectMapper.readTree(sectionAdfJson) instanceof ObjectNode
                    ? (ObjectNode) objectMapper.readTree(sectionAdfJson)
                    : objectMapper.createObjectNode();
                JsonNode sectionContent = sectionDocNode.get("content");
                if (sectionContent != null && sectionContent.isArray()) {
                    for (JsonNode child : sectionContent) {
                        docContent.add(child);
                    }
                }
            }
        }

        String finalDocumentationJson = objectMapper.writeValueAsString(documentationNode);

        // Update Documentation page with complete content (images uploadées sur cette page)
        confluenceClient.updatePageById(documentationPageId, documentationPageTitle, finalDocumentationJson);
        logger.info("Documentation page content updated (ID: {})", documentationPageId);
        // No longer create sub-pages for sections: content already inlined above

    // Générer une seule page avec toutes les vues (toutes les images de diagrammes)
    exportAllViewsSinglePage(workspace, mainPageId);

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

    /**
     * Cleans a specific page tree by deleting the page and all its subpages.
     * 
     * @param pageTitle the title of the page to clean (including all subpages)
     * @throws Exception if cleanup fails
     */
    public void cleanPageTree(String pageTitle) throws Exception {
        logger.info("Starting page tree cleanup for: {}", pageTitle);
        confluenceClient.cleanPageTree(pageTitle);
        logger.info("Page tree cleanup completed for: {}", pageTitle);
    }

    /**
     * Cleans a specific page tree by deleting the page and all its subpages using page ID.
     * 
     * @param pageId the ID of the page to clean (including all subpages)
     * @throws Exception if cleanup fails
     */
    public void cleanPageTreeById(String pageId) throws Exception {
        logger.info("Starting page tree cleanup for ID: {}", pageId);
        confluenceClient.cleanPageTreeById(pageId);
        logger.info("Page tree cleanup completed for ID: {}", pageId);
    }

    public void export(Workspace workspace) throws Exception {
        export(workspace, workspace.getName());
    }
    
    /**
     * Exports the given workspace to Confluence Cloud with a specific parent page.
     * 
     * @param workspace the workspace to export
     * @param parentPageId the parent page ID where a branch subpage will be created
     * @param branchName the branch name
     * @throws Exception if export fails
     */
    public void export(Workspace workspace, String parentPageId, String branchName) throws Exception {
        logger.info("Starting export of workspace '{}' to parent page ID '{}' with branch '{}'", 
            workspace.getName(), parentPageId, branchName);

        // Check if parent page exists
        if (!confluenceClient.pageExists(parentPageId)) {
            logger.info("Parent page with ID '{}' does not exist, creating it...", parentPageId);
            // Create the parent page with minimal content
            Document parentDoc = Document.create()
                .paragraph("This is the root page for Structurizr workspace documentation.");
            String createdPageId = confluenceClient.createOrUpdatePage(
                "Structurizr Workspace",
                convertDocumentToJson(parentDoc)
            );
            logger.info("Created parent page with ID: {}", createdPageId);
        } else {
            logger.info("Parent page with ID '{}' exists, will create branch subpage under it", parentPageId);
        }

        String workspaceId = getWorkspaceId(workspace);
        DiagramExporter diagramExporter = DiagramExporter.fromEnvironment(workspaceId);
        List<File> exportedDiagrams = null;

        if (diagramExporter == null) {
            throw new IllegalStateException("Diagram export via Puppeteer is required but environment variables are not configured. Please define STRUCTURIZR_URL, STRUCTURIZR_USERNAME and STRUCTURIZR_PASSWORD.");
        }

        try {
            logger.info("Exporting diagrams using Playwright...");
            exportedDiagrams = diagramExporter.exportDiagrams(workspace);
            logger.info("Successfully exported {} diagrams", exportedDiagrams.size());
        } catch (Exception e) {
            logger.warn("Diagram export failed, continuing without diagrams: {}", e.getMessage());
        }

        this.exportedDiagrams = exportedDiagrams;
        
        if (exportedDiagrams != null) {
            Function<String, File> diagramResolver = this::getDiagramFile;
            asciiDocConverter.setDiagramResolver(diagramResolver);
            htmlToAdfConverter.setDiagramResolver(diagramResolver);
            logger.info("Configured converters to use {} local diagram files", exportedDiagrams.size());
        }

        // Create branch subpage under parent page
        String branchPageTitle = branchName;
        Document branchDoc = generateWorkspaceDocumentation(workspace, branchName);
        String branchPageId = confluenceClient.createOrUpdatePage(
            branchPageTitle,
            convertDocumentToJson(branchDoc),
            parentPageId
        );
        logger.info("Branch page created/updated with ID: {} under parent: {}", branchPageId, parentPageId);

        // Create Documentation page under branch page with branch suffix
        String documentationPageTitle = "Documentation - " + branchName;
        String documentationPageId = confluenceClient.createOrUpdatePage(
            documentationPageTitle,
            "{\"version\":1,\"type\":\"doc\",\"content\":[]}",
            branchPageId
        );
        logger.info("Documentation page created/updated with ID: {}", documentationPageId);

        // Configure image upload for Documentation page
        ImageUploadManager docImageUploadManager = new ImageUploadManager(confluenceClient);
        htmlToAdfConverter.setImageUploadManager(docImageUploadManager);
        htmlToAdfConverter.setCurrentPageId(documentationPageId);

        Document documentationDoc = Document.create();

        String documentationJson = convertDocumentToJson(documentationDoc);
        ObjectNode documentationNode = objectMapper.readTree(documentationJson) instanceof ObjectNode
            ? (ObjectNode) objectMapper.readTree(documentationJson)
            : objectMapper.createObjectNode();
        ArrayNode docContent = documentationNode.has("content") && documentationNode.get("content").isArray()
            ? (ArrayNode) documentationNode.get("content")
            : documentationNode.putArray("content");

        // Add TOC macro
        ObjectNode tocNode = objectMapper.createObjectNode();
        tocNode.put("type", "extension");
        ObjectNode extAttrs = objectMapper.createObjectNode();
        extAttrs.put("extensionType", "com.atlassian.confluence.macro.core");
        extAttrs.put("extensionKey", "toc");
        tocNode.set("attrs", extAttrs);
        docContent.add(tocNode);

        if (workspace.getDocumentation() != null && !workspace.getDocumentation().getSections().isEmpty()) {
            for (com.structurizr.documentation.Section section : workspace.getDocumentation().getSections()) {
                String filenameFallback = section.getFilename();
                String content = section.getContent();

                String htmlContent;
                String formatName = section.getFormat() != null ? section.getFormat().name() : "";
                if ("AsciiDoc".equalsIgnoreCase(formatName) || "asciidoc".equalsIgnoreCase(formatName)) {
                    String workspaceId2 = getWorkspaceId(workspace);
                    htmlContent = asciiDocConverter.convertToHtml(content, filenameFallback, workspaceId2, branchName);
                } else if ("Markdown".equalsIgnoreCase(formatName) || "md".equalsIgnoreCase(formatName)) {
                    htmlContent = markdownConverter.toHtml(content);
                } else {
                    htmlContent = content;
                }

                htmlToAdfConverter.extractPageTitleOnly(htmlContent);

                String sectionAdfJson = htmlToAdfConverter.convertToAdfJson(htmlContent, filenameFallback);
                ObjectNode sectionDocNode = objectMapper.readTree(sectionAdfJson) instanceof ObjectNode
                    ? (ObjectNode) objectMapper.readTree(sectionAdfJson)
                    : objectMapper.createObjectNode();
                JsonNode sectionContent = sectionDocNode.get("content");
                if (sectionContent != null && sectionContent.isArray()) {
                    for (JsonNode child : sectionContent) {
                        docContent.add(child);
                    }
                }
            }
        }

        String finalDocumentationJson = objectMapper.writeValueAsString(documentationNode);
        confluenceClient.updatePageById(documentationPageId, documentationPageTitle, finalDocumentationJson);
        logger.info("Documentation page content updated (ID: {})", documentationPageId);

        // Create Views page under branch page with branch suffix
        exportAllViewsSinglePage(workspace, branchPageId, branchName);

        // Create ADRs under branch page with branch suffix
        exportDecisions(workspace, branchPageId, branchName);

        logger.info("Workspace export completed successfully");
    }
    
    /**
     * Processes and exports AsciiDoc documentation with diagram injection.
     * 
     * @param workspace the workspace context
     * @param parentPageId the parent page ID
     * @throws Exception if processing or export fails
     */
    /**
     * Exports workspace documentation sections to Confluence.
     * @param workspace the Structurizr workspace
     * @param parentPageId the parent page ID in Confluence
     * @param branchName the branch name for versioning
     */
    public void exportWorkspaceDocumentationSections(Workspace workspace, String parentPageId, String branchName) throws Exception {
        if (workspace.getDocumentation() == null || workspace.getDocumentation().getSections().isEmpty()) {
            logger.info("No documentation sections found in workspace");
            return;
        }

        logger.info("Export des sections de documentation du workspace '{}', {} section(s)", workspace.getName(), workspace.getDocumentation().getSections().size());

        // Export each section as Confluence page
        for (com.structurizr.documentation.Section section : workspace.getDocumentation().getSections()) {
            String filenameFallback = section.getFilename();
            String content = section.getContent();

            String htmlContent;
            String formatName = section.getFormat() != null ? section.getFormat().name() : "";
            
            if ("AsciiDoc".equalsIgnoreCase(formatName) || "asciidoc".equalsIgnoreCase(formatName)) {
                logger.debug("Converting AsciiDoc content for section (filename: {})", filenameFallback);
                String workspaceId = getWorkspaceId(workspace);
                // Passer le filename comme titre indicatif uniquement (log), pas de prise en compte fonctionnelle
                htmlContent = asciiDocConverter.convertToHtml(content, filenameFallback, workspaceId, branchName);
            } else if ("Markdown".equalsIgnoreCase(formatName) || "md".equalsIgnoreCase(formatName)) {
                logger.debug("Markdown content detected for section (filename: {}): converting to HTML for title extraction", filenameFallback);
                // Conversion Markdown robuste avec extensions
                htmlContent = markdownConverter.toHtml(content);
            } else {
                logger.debug("Treating content as HTML for section (filename: {}), format: {}", filenameFallback, formatName);
                htmlContent = content; // Assume HTML ou texte brut
            }

            // Extraire le titre du contenu HTML (premier H1) si disponible
            String extractedTitle = htmlToAdfConverter.extractPageTitleOnly(htmlContent);
            String actualTitle = (extractedTitle != null && !extractedTitle.trim().isEmpty()) ? extractedTitle : filenameFallback;
            
            // Setup image upload manager for this page
            ImageUploadManager imageUploadManager = new ImageUploadManager(confluenceClient);
            htmlToAdfConverter.setImageUploadManager(imageUploadManager);
            
            // Create page first to get the page ID for image uploads
            // Title policy: use first H1 if present; otherwise fallback to filename (no branch prefix)
            String pageTitle = actualTitle;
            String pageId = confluenceClient.createOrUpdatePage(pageTitle, "{\"version\":1,\"type\":\"doc\",\"content\":[]}", parentPageId);
            
            // Set page context for image uploads
            htmlToAdfConverter.setCurrentPageId(pageId);
            
            // Convertir HTML vers ADF JSON pour Confluence avec support des tables natives
            String adfJson = htmlToAdfConverter.convertToAdfJson(htmlContent, actualTitle);

            // Update page with actual content
            confluenceClient.updatePageById(pageId, pageTitle, adfJson);
            logger.info("Section exported to page ID: {} avec le titre: '{}'", filenameFallback, pageId, pageTitle);
        }
    }
    
    


    private String convertDocumentToJson(Document document) throws Exception {
        return objectMapper.writeValueAsString(document);
    }


    
    private Document generateWorkspaceDocumentation(Workspace workspace, String branchName) {
        Document doc = Document.create();

        // Description
        if (workspace.getDescription() != null && !workspace.getDescription().trim().isEmpty()) {
            doc.paragraph(workspace.getDescription());
        }

        // Add views section overview only (pas de sommaire manuel)
        ViewSet views = workspace.getViews();

        // Views Overview (sections uniquement, pas de titre global)
        if (hasViews(views)) {
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
    

    /**
     * Creates a single "Views" page containing all exported view diagrams.
     * Each view is rendered as a diagram image only, without titles or descriptions.
     */
    private void exportAllViewsSinglePage(Workspace workspace, String parentPageId) throws Exception {
        String viewsPageId = confluenceClient.createOrUpdatePage(
            "Views",
            "{\"version\":1,\"type\":\"doc\",\"content\":[]}",
            parentPageId
        );

        // 2) Configurer l’uploader d’images et le contexte de page pour que les images soient attachées à cette page
        ImageUploadManager imageUploadManager = new ImageUploadManager(confluenceClient);
        htmlToAdfConverter.setImageUploadManager(imageUploadManager);
        htmlToAdfConverter.setCurrentPageId(viewsPageId);

        // 3) Construire le contenu ADF de la page "Views" using JSON approach (like documentation)
        Document viewsDoc = Document.create();
        String viewsJson = convertDocumentToJson(viewsDoc);
        ObjectNode viewsNode = objectMapper.readTree(viewsJson) instanceof ObjectNode
            ? (ObjectNode) objectMapper.readTree(viewsJson)
            : objectMapper.createObjectNode();
        ArrayNode viewsContent = viewsNode.has("content") && viewsNode.get("content").isArray()
            ? (ArrayNode) viewsNode.get("content")
            : viewsNode.putArray("content");

        // Add all exported diagrams from /diagrams page
        addExportedDiagramsToContent(viewsContent);

        // 4) Mettre à jour la page avec le contenu complet
        String finalViewsJson = objectMapper.writeValueAsString(viewsNode);
        logger.info("Views page document JSON length: {} characters", finalViewsJson.length());
        logger.debug("Views page document JSON: {}", finalViewsJson);
        
        confluenceClient.updatePageById(viewsPageId, "Views", finalViewsJson);
        logger.info("Created/updated single Views page with all diagrams (pageId: {})", viewsPageId);
    }
    
    /**
     * Creates a single "Views" page containing all exported view diagrams.
     * Version with branch name support.
     * 
     * @param workspace the workspace
     * @param parentPageId the parent page ID
     * @param branchName branch name to add as suffix to page title
     */
    private void exportAllViewsSinglePage(Workspace workspace, String parentPageId, String branchName) throws Exception {

        String viewsPageTitle = "Views - " + branchName;
        String viewsPageId = confluenceClient.createOrUpdatePage(
            viewsPageTitle,
            "{\"version\":1,\"type\":\"doc\",\"content\":[]}",
            parentPageId
        );

        // Configurer l'uploader d'images et le contexte de page
        ImageUploadManager imageUploadManager = new ImageUploadManager(confluenceClient);
        htmlToAdfConverter.setImageUploadManager(imageUploadManager);
        htmlToAdfConverter.setCurrentPageId(viewsPageId);

        // Construire le contenu ADF de la page "Views" using JSON approach (like documentation)
        Document viewsDoc = Document.create();
        String viewsJson = convertDocumentToJson(viewsDoc);
        ObjectNode viewsNode = objectMapper.readTree(viewsJson) instanceof ObjectNode
            ? (ObjectNode) objectMapper.readTree(viewsJson)
            : objectMapper.createObjectNode();
        ArrayNode viewsContent = viewsNode.has("content") && viewsNode.get("content").isArray()
            ? (ArrayNode) viewsNode.get("content")
            : viewsNode.putArray("content");

        // Add all exported diagrams from /diagrams page
        addExportedDiagramsToContent(viewsContent);

        String finalViewsJson = objectMapper.writeValueAsString(viewsNode);
        logger.info("Views page document JSON length: {} characters", finalViewsJson.length());
        logger.debug("Views page document JSON: {}", finalViewsJson);
        
        confluenceClient.updatePageById(viewsPageId, viewsPageTitle, finalViewsJson);
        logger.info("Created/updated Views page with branch suffix (pageId: {})", viewsPageId);
    }

    /**
     * Adds all exported diagram images from the /diagrams page to the document.
     * Iterates through exported diagram files instead of workspace views to ensure
     * all diagrams present on the /diagrams page are included.
     * 
     * @param doc the ADF document to add diagrams to
     * @return the updated document
     */
    /**
     * Adds all exported diagram images from the /diagrams page to the content array.
     * Iterates through exported diagram files instead of workspace views to ensure
     * all diagrams present on the /diagrams page are included.
     * Uses JSON approach (like documentation) to avoid Document serialization issues.
     * 
     * @param content the ArrayNode to add diagram content to
     */
    private void addExportedDiagramsToContent(ArrayNode content) {
        if (exportedDiagrams == null) {
            logger.error("exportedDiagrams is null - diagram export may have failed");
            logger.error("Views page will be empty. Please ensure diagram export succeeds.");
            return;
        }
        
        if (exportedDiagrams.isEmpty()) {
            logger.error("exportedDiagrams list is empty - no diagrams were exported");
            logger.error("Views page will be empty. Please check diagram export logs.");
            return;
        }
        
        logger.info("Adding {} exported diagrams to Views page", exportedDiagrams.size());
        logger.info("Exported diagram files: {}", exportedDiagrams.stream()
            .map(File::getName)
            .collect(java.util.stream.Collectors.joining(", ")));
        
        int processedCount = 0;
        int skippedCount = 0;
        int failedCount = 0;
        
        for (File diagramFile : exportedDiagrams) {
            String filename = diagramFile.getName();
            logger.debug("Processing diagram file: {}", filename);
            
            // Verify file exists and is readable
            if (!diagramFile.exists()) {
                logger.error("Diagram file does not exist: {}", diagramFile.getAbsolutePath());
                failedCount++;
                continue;
            }
            
            if (!diagramFile.canRead()) {
                logger.error("Diagram file is not readable: {}", diagramFile.getAbsolutePath());
                failedCount++;
                continue;
            }
            
            // Extract view key from filename
            // Expected format: structurizr-{workspaceId}-{viewKey}.png or structurizr-{workspaceId}-{viewKey}-key.png
            String viewKey = extractViewKeyFromFilename(filename);
            
            if (viewKey == null) {
                logger.warn("Could not extract view key from filename: {}", filename);
                failedCount++;
                continue;
            }
            
            // Skip key files (only process main diagram files)
            if (filename.endsWith("-key.png")) {
                logger.debug("Skipping key file: {}", filename);
                skippedCount++;
                continue;
            }
            
            logger.info("Adding diagram for view key: {} from file: {}", viewKey, filename);
            
            // Insert the diagram image via local:diagram:KEY placeholder
            try {
                String imgHtml = "<p><img src=\"local:diagram:" + viewKey + "\" alt=\"" + viewKey + "\"></p><p>&nbsp;</p>";
                logger.debug("Created HTML for diagram: {}", imgHtml);
                
                // Convert to ADF JSON and extract content nodes (same approach as documentation)
                String diagramAdfJson = htmlToAdfConverter.convertToAdfJson(imgHtml, viewKey);
                ObjectNode diagramNode = objectMapper.readTree(diagramAdfJson) instanceof ObjectNode
                    ? (ObjectNode) objectMapper.readTree(diagramAdfJson)
                    : objectMapper.createObjectNode();
                JsonNode diagramContent = diagramNode.get("content");
                
                if (diagramContent != null && diagramContent.isArray()) {
                    for (JsonNode child : diagramContent) {
                        content.add(child);
                    }
                    processedCount++;
                    logger.info("Successfully added diagram {} to Views page (total processed: {})", viewKey, processedCount);
                } else {
                    logger.warn("No content found in ADF for diagram: {}", viewKey);
                    failedCount++;
                }
            } catch (Exception e) {
                logger.error("Failed to embed image for view {} from file {}", viewKey, filename, e);
                failedCount++;
            }
        }
        
        logger.info("Views page diagram processing complete: {} processed, {} skipped (key files), {} failed", 
            processedCount, skippedCount, failedCount);
        
        if (processedCount == 0) {
            logger.error("No diagrams were successfully added to Views page!");
            logger.error("Total diagrams in list: {}, Processed: {}, Skipped: {}, Failed: {}", 
                exportedDiagrams.size(), processedCount, skippedCount, failedCount);
        }
    }
    
    /**
     * Extracts the view key from a diagram filename.
     * Expected format: structurizr-{workspaceId}-{viewKey}.png or structurizr-{workspaceId}-{viewKey}-key.png
     * 
     * @param filename the diagram filename
     * @return the view key, or null if it cannot be extracted
     */
    private String extractViewKeyFromFilename(String filename) {
        if (filename == null || !filename.contains("-") || !filename.contains(".")) {
            return null;
        }
        
        // Remove extension
        String nameWithoutExt = filename.substring(0, filename.lastIndexOf('.'));
        
        // Remove "-key" suffix if present
        if (nameWithoutExt.endsWith("-key")) {
            nameWithoutExt = nameWithoutExt.substring(0, nameWithoutExt.length() - 4);
        }
        
        // Expected format: structurizr-{workspaceId}-{viewKey}
        // Find the second dash (after workspaceId)
        int firstDash = nameWithoutExt.indexOf('-');
        if (firstDash < 0) {
            return null;
        }
        
        int secondDash = nameWithoutExt.indexOf('-', firstDash + 1);
        if (secondDash < 0) {
            return null;
        }
        
        // Everything after the second dash is the view key
        return nameWithoutExt.substring(secondDash + 1);
    }

    
    private void exportDecisions(Workspace workspace, String parentPageId, String branchName) throws Exception {
        if (workspace.getDocumentation() == null || workspace.getDocumentation().getDecisions().isEmpty()) {
            logger.info("No architecture decision records found in workspace");
            return;
        }
        
        Collection<Decision> decisions = workspace.getDocumentation().getDecisions();
        logger.info("Exporting {} architecture decision records", decisions.size());
        
        // Create main ADR page (sans H1 dupliqué, laisser Confluence gérer le titre)
        Document adrMainDoc = Document.create()
            .paragraph("This page contains all architecture decision records for this project.");
        
        String adrMainPageTitle = "Architecture Decision Records - " + branchName;
        String adrMainPageId = confluenceClient.createOrUpdatePage(
            adrMainPageTitle, 
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
        Document decisionDoc = Document.create();
        
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
                htmlContent = markdownConverter.toHtml(decision.getContent());
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
     * Gets the exported diagram file for a given view key.
     * 
     * @param viewKey the view key to look for
     * @return the diagram file or null if not found
     */
    public File getDiagramFile(String viewKey) {
        if (exportedDiagrams == null) {
            logger.debug("No exported diagrams available for view key: {}", viewKey);
            return null;
        }
        
        logger.debug("Looking for diagram file for view key: {} among {} exported files", viewKey, exportedDiagrams.size());
        
        for (File diagramFile : exportedDiagrams) {
            String filename = diagramFile.getName();
            logger.debug("Checking diagram file: {}", filename);
            
            // Expected format: structurizr-{workspaceId}-{viewKey}.png
            // Extract the view key using the same logic as extractViewKeyFromFilename
            String extractedViewKey = extractViewKeyFromFilename(filename);
            if (extractedViewKey != null && extractedViewKey.equals(viewKey)) {
                logger.info("Found exact matching diagram file: {} for view key: {}", filename, viewKey);
                return diagramFile;
            }
            
            // Fallback: check if filename contains the view key (for backward compatibility)
            String fileViewKey = filename;
            if (filename.contains(".")) {
                fileViewKey = filename.substring(0, filename.lastIndexOf('.'));
            }
            
            if (fileViewKey.toLowerCase().contains(viewKey.toLowerCase())) {
                logger.info("Found matching diagram file (fallback): {} for view key: {}", filename, viewKey);
                return diagramFile;
            }
        }
        
        logger.warn("No diagram file found for view key: {}", viewKey);
        logger.debug("Available diagram files: {}", exportedDiagrams.stream()
            .map(File::getName)
            .toArray());
        
        return null;
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
     * Combines two ADF documents by merging their content.
     */
    private Document combineDocuments(Document base, Document addition) {
        try {
            ObjectNode baseNode = objectMapper.valueToTree(base);
            ObjectNode addNode = objectMapper.valueToTree(addition);

            ArrayNode baseContent;
            JsonNode baseContentNode = baseNode.get("content");
            if (baseContentNode != null && baseContentNode.isArray()) {
                baseContent = (ArrayNode) baseContentNode;
            } else {
                baseContent = baseNode.putArray("content");
            }

            JsonNode addContentNode = addNode.get("content");
            if (addContentNode != null && addContentNode.isArray()) {
                for (JsonNode child : addContentNode) {
                    baseContent.add(child);
                }
            }

            return objectMapper.treeToValue(baseNode, Document.class);
        } catch (Exception e) {
            logger.warn("Failed to merge ADF documents, keeping base content only", e);
            return base;
        }
    }
}