package arnaudroubinet.structurizr.confluence.exporter;

import arnaudroubinet.structurizr.confluence.client.ConfluenceClient;
import arnaudroubinet.structurizr.confluence.processor.HtmlToAdfConverter;
import arnaudroubinet.structurizr.confluence.processor.ImageUploadManager;
import com.atlassian.adf.Document;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.structurizr.Workspace;
import com.structurizr.view.ViewSet;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles export of workspace views and diagrams to Confluence. Separates view export logic from
 * main export orchestration.
 */
public class ViewExporter {

  private static final Logger logger = LoggerFactory.getLogger(ViewExporter.class);

  private final ConfluenceClient confluenceClient;
  private final ObjectMapper objectMapper;
  private final HtmlToAdfConverter htmlToAdfConverter;
  private List<File> exportedDiagrams;

  public ViewExporter(
      ConfluenceClient confluenceClient,
      ObjectMapper objectMapper,
      HtmlToAdfConverter htmlToAdfConverter) {
    this.confluenceClient = confluenceClient;
    this.objectMapper = objectMapper;
    this.htmlToAdfConverter = htmlToAdfConverter;
  }

  /**
   * Sets the list of exported diagram files.
   *
   * @param exportedDiagrams the list of diagram files
   */
  public void setExportedDiagrams(List<File> exportedDiagrams) {
    this.exportedDiagrams = exportedDiagrams;
  }

  /**
   * Creates a single "Views" page containing all exported view diagrams.
   *
   * @param workspace the workspace
   * @param parentPageId the parent page ID
   * @throws Exception if export fails
   */
  public void exportAllViewsSinglePage(Workspace workspace, String parentPageId) throws Exception {
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
   * @throws Exception if export fails
   */
  public void exportAllViewsSinglePage(Workspace workspace, String parentPageId, String branchName)
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
    List<String> includedViewKeys = new ArrayList<>();

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
  private void addFallbackDiagramPlaceholders(ArrayNode content, List<String> viewKeys) {
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

  private String convertDocumentToJson(Document document) throws Exception {
    return objectMapper.writeValueAsString(document);
  }
}
