package arnaudroubinet.structurizr.confluence;

import arnaudroubinet.structurizr.confluence.client.ConfluenceClient;
import arnaudroubinet.structurizr.confluence.client.ConfluenceConfig;
import arnaudroubinet.structurizr.confluence.client.StructurizrConfig;
import arnaudroubinet.structurizr.confluence.client.StructurizrWorkspaceLoader;
import arnaudroubinet.structurizr.confluence.exporter.AdrExporter;
import arnaudroubinet.structurizr.confluence.generator.DocumentGenerator;
import arnaudroubinet.structurizr.confluence.processor.AsciiDocConverter;
import arnaudroubinet.structurizr.confluence.processor.DiagramExporter;
import arnaudroubinet.structurizr.confluence.processor.HtmlToAdfConverter;
import arnaudroubinet.structurizr.confluence.processor.ImageUploadManager;
import arnaudroubinet.structurizr.confluence.processor.MarkdownConverter;
import com.atlassian.adf.Document;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.structurizr.Workspace;
import com.structurizr.api.StructurizrClientException;
import com.structurizr.model.*;
import com.structurizr.view.*;
import java.io.File;
import java.util.List;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Exports Structurizr workspace documentation and ADRs to Confluence Cloud in Atlassian Document
 * Format (ADF). Can load workspaces from Structurizr on-premise instances or work with provided
 * workspace objects.
 */
public class ConfluenceExporter {

  private static final Logger logger = LoggerFactory.getLogger(ConfluenceExporter.class);

  // Documentation format constants
  private static final String FORMAT_ASCIIDOC = "AsciiDoc";
  private static final String FORMAT_ASCIIDOC_LOWER = "asciidoc";
  private static final String FORMAT_MARKDOWN = "Markdown";
  private static final String FORMAT_MARKDOWN_SHORT = "md";

  private final ConfluenceClient confluenceClient;
  private final ObjectMapper objectMapper;
  private final StructurizrWorkspaceLoader workspaceLoader;
  private final HtmlToAdfConverter htmlToAdfConverter;
  private final AsciiDocConverter asciiDocConverter;
  private final MarkdownConverter markdownConverter;
  private final DocumentGenerator documentGenerator;
  private final AdrExporter adrExporter;
  private List<File> exportedDiagrams;

  /** Creates an exporter that loads workspaces from a Structurizr on-premise instance. */
  public ConfluenceExporter(ConfluenceConfig confluenceConfig, StructurizrConfig structurizrConfig)
      throws StructurizrClientException {
    this.confluenceClient = new ConfluenceClient(confluenceConfig);
    this.objectMapper = new ObjectMapper();
    this.workspaceLoader = new StructurizrWorkspaceLoader(structurizrConfig);
    this.htmlToAdfConverter = new HtmlToAdfConverter();
    this.asciiDocConverter = new AsciiDocConverter();
    this.markdownConverter = new MarkdownConverter();
    this.documentGenerator = new DocumentGenerator();
    this.adrExporter =
        new AdrExporter(
            confluenceClient,
            objectMapper,
            htmlToAdfConverter,
            asciiDocConverter,
            markdownConverter);
  }

  /** Creates an exporter for use with provided workspace objects (original behavior). */
  public ConfluenceExporter(ConfluenceConfig confluenceConfig) {
    this.confluenceClient = new ConfluenceClient(confluenceConfig);
    this.objectMapper = new ObjectMapper();
    this.workspaceLoader = null;
    this.htmlToAdfConverter = new HtmlToAdfConverter();
    this.asciiDocConverter = new AsciiDocConverter();
    this.markdownConverter = new MarkdownConverter();
    this.documentGenerator = new DocumentGenerator();
    this.adrExporter =
        new AdrExporter(
            confluenceClient,
            objectMapper,
            htmlToAdfConverter,
            asciiDocConverter,
            markdownConverter);
  }

  /**
   * Exports a workspace loaded from the configured Structurizr instance.
   *
   * @throws Exception if export fails
   */
  public void exportFromStructurizr() throws Exception {
    if (workspaceLoader == null) {
      throw new IllegalStateException(
          "No Structurizr configuration provided. Use the constructor with StructurizrConfig or call export(Workspace) directly.");
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
      throw new IllegalStateException(
          "No Structurizr configuration provided. Use the constructor with StructurizrConfig or call export(Workspace) directly.");
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
    logger.info(
        "Starting export of workspace '{}' (branch '{}') to Confluence",
        workspace.getName(),
        branchName);

    String workspaceId = getWorkspaceId(workspace);
    DiagramExporter diagramExporter = DiagramExporter.fromEnvironment(workspaceId);
    List<File> exportedDiagrams = null;

    if (diagramExporter == null) {
      throw new IllegalStateException(
          "Diagram export via Puppeteer is required but environment variables are not configured. Please define STRUCTURIZR_URL, STRUCTURIZR_USERNAME and STRUCTURIZR_PASSWORD.");
    }

    try {
      logger.info("Exporting diagrams using Playwright...");
      exportedDiagrams = diagramExporter.exportDiagrams(workspace);
      logger.info("Successfully exported {} diagrams", exportedDiagrams.size());
    } catch (Exception e) {
      logger.warn("Diagram export failed, continuing without diagrams: {}", e.getMessage());
      // Don't fail the entire process if diagram export fails
      // throw new IllegalStateException("Diagram export via Playwright failed. Stopping process.",
      // e);
    }

    this.exportedDiagrams = exportedDiagrams;

    if (exportedDiagrams != null) {
      Function<String, File> diagramResolver = this::getDiagramFile;
      asciiDocConverter.setDiagramResolver(diagramResolver);
      htmlToAdfConverter.setDiagramResolver(diagramResolver);
      logger.info("Configured converters to use {} local diagram files", exportedDiagrams.size());
    }

    String mainPageTitle = branchName;
    Document mainDoc = documentGenerator.generateWorkspaceDocumentation(workspace, branchName);
    String mainPageId =
        confluenceClient.createOrUpdatePage(mainPageTitle, convertDocumentToJson(mainDoc));

    logger.info("Main page created/updated with ID: {}", mainPageId);

    String documentationPageTitle = "Documentation";
    String documentationPageId =
        confluenceClient.createOrUpdatePage(
            documentationPageTitle, "{\"version\":1,\"type\":\"doc\",\"content\":[]}", mainPageId);
    logger.info("Documentation page created/updated with ID: {}", documentationPageId);

    // Configurer l’upload d’images pour la page Documentation
    ImageUploadManager docImageUploadManager = new ImageUploadManager(confluenceClient);
    htmlToAdfConverter.setImageUploadManager(docImageUploadManager);
    htmlToAdfConverter.setCurrentPageId(documentationPageId);

    Document documentationDoc = Document.create();

    String documentationJson = convertDocumentToJson(documentationDoc);
    ObjectNode documentationNode =
        objectMapper.readTree(documentationJson) instanceof ObjectNode
            ? (ObjectNode) objectMapper.readTree(documentationJson)
            : objectMapper.createObjectNode();
    ArrayNode docContent =
        documentationNode.has("content") && documentationNode.get("content").isArray()
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

    if (workspace.getDocumentation() != null
        && !workspace.getDocumentation().getSections().isEmpty()) {
      for (com.structurizr.documentation.Section section :
          workspace.getDocumentation().getSections()) {
        String filenameFallback = section.getFilename();
        String content = section.getContent();

        String htmlContent;
        String formatName = section.getFormat() != null ? section.getFormat().name() : "";
        if (isAsciiDocFormat(formatName)) {
          String workspaceId2 = getWorkspaceId(workspace);
          htmlContent =
              asciiDocConverter.convertToHtml(content, filenameFallback, workspaceId2, branchName);
        } else if (isMarkdownFormat(formatName)) {
          htmlContent = markdownConverter.toHtml(content);
        } else {
          htmlContent = content;
        }

        htmlToAdfConverter.extractPageTitleOnly(htmlContent);

        // Convert section HTML to ADF JSON (avec post-traitements)
        String sectionAdfJson = htmlToAdfConverter.convertToAdfJson(htmlContent, filenameFallback);
        ObjectNode sectionDocNode =
            objectMapper.readTree(sectionAdfJson) instanceof ObjectNode
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
    confluenceClient.updatePageById(
        documentationPageId, documentationPageTitle, finalDocumentationJson);
    logger.info("Documentation page content updated (ID: {})", documentationPageId);
    // No longer create sub-pages for sections: content already inlined above

    // Générer une seule page avec toutes les vues (toutes les images de diagrammes)
    exportAllViewsSinglePage(workspace, mainPageId);

    // Générer les ADRs
    adrExporter.exportDecisions(workspace, mainPageId, branchName);

    logger.info("Workspace export completed successfully");
  }

  /** Closes resources used by the exporter. */
  public void close() {
    if (asciiDocConverter != null) {
      asciiDocConverter.close();
    }
  }

  /** Returns the ConfluenceClient for direct access to Confluence operations. */
  public ConfluenceClient getConfluenceClient() {
    return confluenceClient;
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
    logger.info(
        "Starting export of workspace '{}' to parent page ID '{}' with branch '{}'",
        workspace.getName(),
        parentPageId,
        branchName);

    // Check if parent page exists
    if (!confluenceClient.pageExists(parentPageId)) {
      logger.info("Parent page with ID '{}' does not exist, creating it...", parentPageId);
      // Create the parent page with minimal content
      Document parentDoc =
          Document.create()
              .paragraph("This is the root page for Structurizr workspace documentation.");
      String createdPageId =
          confluenceClient.createOrUpdatePage(
              "Structurizr Workspace", convertDocumentToJson(parentDoc));
      logger.info("Created parent page with ID: {}", createdPageId);
    } else {
      logger.info(
          "Parent page with ID '{}' exists, will create branch subpage under it", parentPageId);
    }

    String workspaceId = getWorkspaceId(workspace);
    DiagramExporter diagramExporter = DiagramExporter.fromEnvironment(workspaceId);
    List<File> exportedDiagrams = null;

    if (diagramExporter == null) {
      throw new IllegalStateException(
          "Diagram export via Puppeteer is required but environment variables are not configured. Please define STRUCTURIZR_URL, STRUCTURIZR_USERNAME and STRUCTURIZR_PASSWORD.");
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
    Document branchDoc = documentGenerator.generateWorkspaceDocumentation(workspace, branchName);
    String branchPageId =
        confluenceClient.createOrUpdatePage(
            branchPageTitle, convertDocumentToJson(branchDoc), parentPageId);
    logger.info(
        "Branch page created/updated with ID: {} under parent: {}", branchPageId, parentPageId);

    // Create Documentation page under branch page with branch suffix
    String documentationPageTitle = "Documentation - " + branchName;
    String documentationPageId =
        confluenceClient.createOrUpdatePage(
            documentationPageTitle,
            "{\"version\":1,\"type\":\"doc\",\"content\":[]}",
            branchPageId);
    logger.info("Documentation page created/updated with ID: {}", documentationPageId);

    // Configure image upload for Documentation page
    ImageUploadManager docImageUploadManager = new ImageUploadManager(confluenceClient);
    htmlToAdfConverter.setImageUploadManager(docImageUploadManager);
    htmlToAdfConverter.setCurrentPageId(documentationPageId);

    Document documentationDoc = Document.create();

    String documentationJson = convertDocumentToJson(documentationDoc);
    ObjectNode documentationNode =
        objectMapper.readTree(documentationJson) instanceof ObjectNode
            ? (ObjectNode) objectMapper.readTree(documentationJson)
            : objectMapper.createObjectNode();
    ArrayNode docContent =
        documentationNode.has("content") && documentationNode.get("content").isArray()
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

    if (workspace.getDocumentation() != null
        && !workspace.getDocumentation().getSections().isEmpty()) {
      for (com.structurizr.documentation.Section section :
          workspace.getDocumentation().getSections()) {
        String filenameFallback = section.getFilename();
        String content = section.getContent();

        String htmlContent;
        String formatName = section.getFormat() != null ? section.getFormat().name() : "";
        if (isAsciiDocFormat(formatName)) {
          String workspaceId2 = getWorkspaceId(workspace);
          htmlContent =
              asciiDocConverter.convertToHtml(content, filenameFallback, workspaceId2, branchName);
        } else if (isMarkdownFormat(formatName)) {
          htmlContent = markdownConverter.toHtml(content);
        } else {
          htmlContent = content;
        }

        htmlToAdfConverter.extractPageTitleOnly(htmlContent);

        String sectionAdfJson = htmlToAdfConverter.convertToAdfJson(htmlContent, filenameFallback);
        ObjectNode sectionDocNode =
            objectMapper.readTree(sectionAdfJson) instanceof ObjectNode
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
    confluenceClient.updatePageById(
        documentationPageId, documentationPageTitle, finalDocumentationJson);
    logger.info("Documentation page content updated (ID: {})", documentationPageId);

    // Create Views page under branch page with branch suffix
    exportAllViewsSinglePage(workspace, branchPageId, branchName);

    // Create ADRs under branch page with branch suffix
    adrExporter.exportDecisions(workspace, branchPageId, branchName);

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
   *
   * @param workspace the Structurizr workspace
   * @param parentPageId the parent page ID in Confluence
   * @param branchName the branch name for versioning
   */
  public void exportWorkspaceDocumentationSections(
      Workspace workspace, String parentPageId, String branchName) throws Exception {
    if (workspace.getDocumentation() == null
        || workspace.getDocumentation().getSections().isEmpty()) {
      logger.info("No documentation sections found in workspace");
      return;
    }

    logger.info(
        "Export des sections de documentation du workspace '{}', {} section(s)",
        workspace.getName(),
        workspace.getDocumentation().getSections().size());

    // Export each section as Confluence page
    for (com.structurizr.documentation.Section section :
        workspace.getDocumentation().getSections()) {
      String filenameFallback = section.getFilename();
      String content = section.getContent();

      String htmlContent;
      String formatName = section.getFormat() != null ? section.getFormat().name() : "";

      if (isAsciiDocFormat(formatName)) {
        logger.debug("Converting AsciiDoc content for section (filename: {})", filenameFallback);
        String workspaceId = getWorkspaceId(workspace);
        // Passer le filename comme titre indicatif uniquement (log), pas de prise en compte
        // fonctionnelle
        htmlContent =
            asciiDocConverter.convertToHtml(content, filenameFallback, workspaceId, branchName);
      } else if (isMarkdownFormat(formatName)) {
        logger.debug(
            "Markdown content detected for section (filename: {}): converting to HTML for title extraction",
            filenameFallback);
        // Conversion Markdown robuste avec extensions
        htmlContent = markdownConverter.toHtml(content);
      } else {
        logger.debug(
            "Treating content as HTML for section (filename: {}), format: {}",
            filenameFallback,
            formatName);
        htmlContent = content; // Assume HTML ou texte brut
      }

      // Extraire le titre du contenu HTML (premier H1) si disponible
      String extractedTitle = htmlToAdfConverter.extractPageTitleOnly(htmlContent);
      String actualTitle =
          (extractedTitle != null && !extractedTitle.trim().isEmpty())
              ? extractedTitle
              : filenameFallback;

      // Setup image upload manager for this page
      ImageUploadManager imageUploadManager = new ImageUploadManager(confluenceClient);
      htmlToAdfConverter.setImageUploadManager(imageUploadManager);

      // Create page first to get the page ID for image uploads
      // Title policy: use first H1 if present; otherwise fallback to filename (no branch prefix)
      String pageTitle = actualTitle;
      String pageId =
          confluenceClient.createOrUpdatePage(
              pageTitle, "{\"version\":1,\"type\":\"doc\",\"content\":[]}", parentPageId);

      // Set page context for image uploads
      htmlToAdfConverter.setCurrentPageId(pageId);

      // Convertir HTML vers ADF JSON pour Confluence avec support des tables natives
      String adfJson = htmlToAdfConverter.convertToAdfJson(htmlContent, actualTitle);

      // Update page with actual content
      confluenceClient.updatePageById(pageId, pageTitle, adfJson);
      logger.info(
          "Section exported to page ID: {} avec le titre: '{}'",
          filenameFallback,
          pageId,
          pageTitle);
    }
  }

  private String convertDocumentToJson(Document document) throws Exception {
    return objectMapper.writeValueAsString(document);
  }

  /**
   * Creates a single "Views" page containing all exported view diagrams. Each view is rendered as a
   * diagram image only, without titles or descriptions.
   */
  private void exportAllViewsSinglePage(Workspace workspace, String parentPageId) throws Exception {
    ViewSet views = workspace.getViews();
    logger.info(
        "[ViewsExport] Single page export (no branch) - counts => SystemLandscape: {} | SystemContext: {} | Container: {} | Component: {} | Deployment: {}",
        views.getSystemLandscapeViews().size(),
        views.getSystemContextViews().size(),
        views.getContainerViews().size(),
        views.getComponentViews().size(),
        views.getDeploymentViews().size());

    String viewsPageId =
        confluenceClient.createOrUpdatePage(
            "Views", "{\"version\":1,\"type\":\"doc\",\"content\":[]}", parentPageId);

    ImageUploadManager imageUploadManager = new ImageUploadManager(confluenceClient);
    htmlToAdfConverter.setImageUploadManager(imageUploadManager);
    htmlToAdfConverter.setCurrentPageId(viewsPageId);

    Document viewsDoc = Document.create();
    String viewsJson = convertDocumentToJson(viewsDoc);
    ObjectNode viewsNode =
        objectMapper.readTree(viewsJson) instanceof ObjectNode
            ? (ObjectNode) objectMapper.readTree(viewsJson)
            : objectMapper.createObjectNode();
    ArrayNode viewsContent =
        viewsNode.has("content") && viewsNode.get("content").isArray()
            ? (ArrayNode) viewsNode.get("content")
            : viewsNode.putArray("content");

    addExportedDiagramsToContent(viewsContent);

    int nodeCount = viewsContent.size();
    logger.info("[ViewsExport] Generated ADF nodes (no branch): {}", nodeCount);
    if (nodeCount == 0) {
      logger.warn(
          "[ViewsExport] No content nodes added to 'Views' page (no branch version). Page will appear empty.");
    }

    String finalViewsJson = objectMapper.writeValueAsString(viewsNode);
    logger.info(
        "[ViewsExport] Views page JSON length (no branch): {} chars", finalViewsJson.length());
    if (logger.isDebugEnabled()) {
      logger.debug("[ViewsExport] Views page JSON (no branch): {}", finalViewsJson);
    }
    confluenceClient.updatePageById(viewsPageId, "Views", finalViewsJson);
    logger.info("Created/updated single Views page with all diagrams (pageId: {})", viewsPageId);
  }

  /**
   * Creates a single "Views" page containing all exported view diagrams. Version with branch name
   * support.
   *
   * @param workspace the workspace
   * @param parentPageId the parent page ID
   * @param branchName branch name to add as suffix to page title
   */
  private void exportAllViewsSinglePage(Workspace workspace, String parentPageId, String branchName)
      throws Exception {
    ViewSet views = workspace.getViews();
    logger.info(
        "[ViewsExport] Branch export '{}' - counts => SystemLandscape: {} | SystemContext: {} | Container: {} | Component: {} | Deployment: {}",
        branchName,
        views.getSystemLandscapeViews().size(),
        views.getSystemContextViews().size(),
        views.getContainerViews().size(),
        views.getComponentViews().size(),
        views.getDeploymentViews().size());

    String viewsPageTitle = "Views - " + branchName;
    String viewsPageId =
        confluenceClient.createOrUpdatePage(
            viewsPageTitle, "{\"version\":1,\"type\":\"doc\",\"content\":[]}", parentPageId);

    ImageUploadManager imageUploadManager = new ImageUploadManager(confluenceClient);
    htmlToAdfConverter.setImageUploadManager(imageUploadManager);
    htmlToAdfConverter.setCurrentPageId(viewsPageId);

    Document viewsDoc = Document.create();
    String viewsJson = convertDocumentToJson(viewsDoc);
    ObjectNode viewsNode =
        objectMapper.readTree(viewsJson) instanceof ObjectNode
            ? (ObjectNode) objectMapper.readTree(viewsJson)
            : objectMapper.createObjectNode();
    ArrayNode viewsContent =
        viewsNode.has("content") && viewsNode.get("content").isArray()
            ? (ArrayNode) viewsNode.get("content")
            : viewsNode.putArray("content");

    addExportedDiagramsToContent(viewsContent);

    int nodeCount = viewsContent.size();
    logger.info("[ViewsExport] Generated ADF nodes (branch '{}'): {}", branchName, nodeCount);
    if (nodeCount == 0) {
      logger.warn(
          "[ViewsExport] No content nodes added to 'Views - {}' page. Page will appear empty.",
          branchName);
    }

    String finalViewsJson = objectMapper.writeValueAsString(viewsNode);
    logger.info(
        "[ViewsExport] Views page JSON length (branch '{}'): {} chars",
        branchName,
        finalViewsJson.length());
    if (logger.isDebugEnabled()) {
      logger.debug("[ViewsExport] Views page JSON (branch '{}'): {}", branchName, finalViewsJson);
    }
    confluenceClient.updatePageById(viewsPageId, viewsPageTitle, finalViewsJson);
    logger.info("Created/updated Views page with branch suffix (pageId: {})", viewsPageId);
  }

  /**
   * Adds all exported diagram images from the /diagrams page to the document. Iterates through
   * exported diagram files instead of workspace views to ensure all diagrams present on the
   * /diagrams page are included.
   *
   * @param doc the ADF document to add diagrams to
   * @return the updated document
   */
  /**
   * Adds all exported diagram images from the /diagrams page to the content array. Iterates through
   * exported diagram files instead of workspace views to ensure all diagrams present on the
   * /diagrams page are included. Uses JSON approach (like documentation) to avoid Document
   * serialization issues.
   *
   * @param content the ArrayNode to add diagram content to
   */
  private void addExportedDiagramsToContent(ArrayNode content) {
    if (exportedDiagrams == null) {
      logger.error(
          "[ViewsExport] exportedDiagrams is null - diagram export may have failed; page will be empty");
      return;
    }
    if (exportedDiagrams.isEmpty()) {
      logger.error(
          "[ViewsExport] exportedDiagrams is empty - no diagrams exported; page will be empty");
      return;
    }

    logger.info("[ViewsExport] Building combined HTML for {} diagrams", exportedDiagrams.size());
    StringBuilder html = new StringBuilder();
    int skippedKeys = 0;
    int included = 0;
    java.util.List<String> includedViewKeys = new java.util.ArrayList<>();

    for (File diagramFile : exportedDiagrams) {
      String filename = diagramFile.getName();
      if (filename.endsWith("-key.png")) { // skip legend/key images
        skippedKeys++;
        continue;
      }
      String viewKey = extractViewKeyFromFilename(filename);
      if (viewKey == null) {
        logger.warn(
            "[ViewsExport] Cannot extract view key from filename '{}' -> skipping", filename);
        continue;
      }
      html.append("<p><img src=\"local:diagram:")
          .append(viewKey)
          .append("\" alt=\"")
          .append(viewKey)
          .append("\"></p>");
      included++;
      includedViewKeys.add(viewKey);
    }

    logger.info(
        "[ViewsExport] Included {} diagram(s), skipped {} '-key' files", included, skippedKeys);
    if (included == 0) {
      logger.warn("[ViewsExport] No base diagram images included in combined HTML -> aborting");
      return;
    }

    // Single conversion attempt
    String combinedHtml = html.toString();
    logger.debug("[ViewsExport] Combined HTML length: {} chars", combinedHtml.length());
    try {
      String combinedAdfJson =
          htmlToAdfConverter.convertToAdfJson(combinedHtml, "All Views Diagrams");
      logger.debug("[ViewsExport] Combined ADF JSON length: {} chars", combinedAdfJson.length());
      ObjectNode combinedNode =
          objectMapper.readTree(combinedAdfJson) instanceof ObjectNode
              ? (ObjectNode) objectMapper.readTree(combinedAdfJson)
              : objectMapper.createObjectNode();
      JsonNode combinedContent = combinedNode.get("content");
      int before = content.size();
      if (combinedContent != null && combinedContent.isArray()) {
        for (JsonNode child : combinedContent) {
          content.add(child);
        }
      }
      int added = content.size() - before;
      logger.info(
          "[ViewsExport] Added {} ADF node(s) to Views page from combined conversion (expected >= {})",
          added,
          included);
      if (added == 0) {
        logger.warn(
            "[ViewsExport] Combined conversion produced 0 nodes; activating fallback placeholder strategy");
        addFallbackDiagramPlaceholders(content, includedViewKeys);
      }
    } catch (Exception e) {
      logger.error(
          "[ViewsExport] Combined conversion failed: {} - activating fallback placeholders",
          e.getMessage(),
          e);
      addFallbackDiagramPlaceholders(content, includedViewKeys);
    }
  }

  /** Fallback: insère des paragraphes placeholder si la conversion ADF ne retourne rien. */
  private void addFallbackDiagramPlaceholders(ArrayNode content, java.util.List<String> viewKeys) {
    for (String vk : viewKeys) {
      ObjectNode paragraph = objectMapper.createObjectNode();
      paragraph.put("type", "paragraph");
      ArrayNode pContent = paragraph.putArray("content");
      ObjectNode text = objectMapper.createObjectNode();
      text.put("type", "text");
      text.put("text", "[diagram: " + vk + "] (placeholder - conversion failed)");
      pContent.add(text);
      content.add(paragraph);
    }
    logger.info("[ViewsExport] Inserted {} placeholder paragraph(s) for diagrams", viewKeys.size());
  }

  /**
   * Extracts the view key from a diagram filename. Expected format:
   * structurizr-{workspaceId}-{viewKey}.png or structurizr-{workspaceId}-{viewKey}-key.png
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

    logger.debug(
        "Looking for diagram file for view key: {} among {} exported files",
        viewKey,
        exportedDiagrams.size());

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
        logger.info(
            "Found matching diagram file (fallback): {} for view key: {}", filename, viewKey);
        return diagramFile;
      }
    }

    logger.warn("No diagram file found for view key: {}", viewKey);
    logger.debug(
        "Available diagram files: {}", exportedDiagrams.stream().map(File::getName).toArray());

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
   * Checks if the format is AsciiDoc.
   *
   * @param formatName the format name to check
   * @return true if format is AsciiDoc
   */
  private boolean isAsciiDocFormat(String formatName) {
    return FORMAT_ASCIIDOC.equalsIgnoreCase(formatName)
        || FORMAT_ASCIIDOC_LOWER.equalsIgnoreCase(formatName);
  }

  /**
   * Checks if the format is Markdown.
   *
   * @param formatName the format name to check
   * @return true if format is Markdown
   */
  private boolean isMarkdownFormat(String formatName) {
    return FORMAT_MARKDOWN.equalsIgnoreCase(formatName)
        || FORMAT_MARKDOWN_SHORT.equalsIgnoreCase(formatName);
  }
}
