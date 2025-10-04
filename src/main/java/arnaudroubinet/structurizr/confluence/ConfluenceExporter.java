package arnaudroubinet.structurizr.confluence;

import arnaudroubinet.structurizr.confluence.client.ConfluenceClient;
import arnaudroubinet.structurizr.confluence.client.ConfluenceConfig;
import arnaudroubinet.structurizr.confluence.client.StructurizrConfig;
import arnaudroubinet.structurizr.confluence.client.StructurizrWorkspaceLoader;
import arnaudroubinet.structurizr.confluence.exporter.AdrExporter;
import arnaudroubinet.structurizr.confluence.exporter.DocumentationSectionExporter;
import arnaudroubinet.structurizr.confluence.exporter.ViewExporter;
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
  private final DocumentationSectionExporter documentationSectionExporter;
  private final ViewExporter viewExporter;
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
    this.documentationSectionExporter =
        new DocumentationSectionExporter(
            confluenceClient, htmlToAdfConverter, asciiDocConverter, markdownConverter);
    this.viewExporter = new ViewExporter(confluenceClient, objectMapper, htmlToAdfConverter);
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
    this.documentationSectionExporter =
        new DocumentationSectionExporter(
            confluenceClient, htmlToAdfConverter, asciiDocConverter, markdownConverter);
    this.viewExporter = new ViewExporter(confluenceClient, objectMapper, htmlToAdfConverter);
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
    viewExporter.setExportedDiagrams(exportedDiagrams);

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
    viewExporter.exportAllViewsSinglePage(workspace, mainPageId);

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
    viewExporter.setExportedDiagrams(exportedDiagrams);

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
    viewExporter.exportAllViewsSinglePage(workspace, branchPageId, branchName);

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
    documentationSectionExporter.exportWorkspaceDocumentationSections(
        workspace, parentPageId, branchName);
  }

  private String convertDocumentToJson(Document document) throws Exception {
    return objectMapper.writeValueAsString(document);
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
      // Extract the view key inline
      String extractedViewKey = extractViewKeyFromFile(filename);
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
   * Extracts the view key from a diagram filename for use in getDiagramFile.
   *
   * @param filename the diagram filename
   * @return the view key, or null if it cannot be extracted
   */
  private String extractViewKeyFromFile(String filename) {
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
    int firstDash = nameWithoutExt.indexOf('-');
    if (firstDash < 0) {
      return null;
    }

    int secondDash = nameWithoutExt.indexOf('-', firstDash + 1);
    if (secondDash < 0) {
      return null;
    }

    return nameWithoutExt.substring(secondDash + 1);
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
