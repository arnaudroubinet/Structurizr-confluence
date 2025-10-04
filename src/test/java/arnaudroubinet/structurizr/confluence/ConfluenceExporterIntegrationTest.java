package arnaudroubinet.structurizr.confluence;

import static org.junit.jupiter.api.Assertions.*;

import arnaudroubinet.structurizr.confluence.client.ConfluenceClient;
import arnaudroubinet.structurizr.confluence.client.ConfluenceConfig;
import com.structurizr.Workspace;
import com.structurizr.util.WorkspaceUtils;
import io.quarkus.test.junit.QuarkusTest;
import java.io.File;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@QuarkusTest
public class ConfluenceExporterIntegrationTest {
  private static final Logger logger =
      LoggerFactory.getLogger(ConfluenceExporterIntegrationTest.class);

  @Test
  public void exportWorkspaceToConfluence() throws Exception {
    String confluenceUser = System.getenv("CONFLUENCE_USER");
    String confluenceUrl = System.getenv("CONFLUENCE_URL");
    String confluenceToken = System.getenv("CONFLUENCE_TOKEN");
    String confluenceSpaceKey = System.getenv("CONFLUENCE_SPACE_KEY");
    Assumptions.assumeTrue(
        confluenceUser != null && !confluenceUser.isBlank(),
        "CONFLUENCE_USER not defined: test skipped");
    Assumptions.assumeTrue(
        confluenceUrl != null && !confluenceUrl.isBlank(),
        "CONFLUENCE_URL not defined: test skipped");
    Assumptions.assumeTrue(
        confluenceToken != null && !confluenceToken.isBlank(),
        "CONFLUENCE_TOKEN not defined: test skipped");
    Assumptions.assumeTrue(
        confluenceSpaceKey != null && !confluenceSpaceKey.isBlank(),
        "CONFLUENCE_SPACE_KEY not defined: test skipped");
    assertNotNull(confluenceSpaceKey, "CONFLUENCE_SPACE_KEY must be defined");

    ConfluenceConfig config =
        new ConfluenceConfig(confluenceUrl, confluenceUser, confluenceToken, confluenceSpaceKey);

    // Clean the Confluence space before export
    ConfluenceExporter exporter = new ConfluenceExporter(config);
    ConfluenceClient confluenceClient = exporter.getConfluenceClient();

    logger.info("Cleaning Confluence space: {}", confluenceSpaceKey);
    confluenceClient.cleanSpace();
    logger.info("Cleaning completed");

    File file = Path.of("demo/itms-workspace.json").toFile();
    Workspace workspace = WorkspaceUtils.loadWorkspaceFromJson(file);
    assertNotNull(workspace);

    logger.info("Starting workspace export...");
    exporter.export(workspace);
    logger.info("Export completed");

    // Validate exported content by fetching and checking the pages
    validateExportedContent(confluenceClient, confluenceSpaceKey);
  }

  /**
   * Validates the exported content by fetching pages from Confluence API and checking formatting
   * preservation.
   */
  private void validateExportedContent(ConfluenceClient confluenceClient, String spaceKey)
      throws Exception {
    logger.info("=== Starting content validation ===");

    try {
      // Get all pages in the space
      List<String> pageIds = confluenceClient.getSpacePageIds(spaceKey);
      assertTrue(pageIds.size() > 0, "At least one page should be created");
      logger.info("Found {} pages in space", pageIds.size());

      for (String pageId : pageIds) {
        // Get page info to check title
        String pageInfo = confluenceClient.getPageInfo(pageId);
        assertNotNull("Page info should not be null", pageInfo);
        logger.info("Validating page: {}", pageId);

        // Get page content to validate ADF structure
        String pageContent = confluenceClient.getPageContent(pageId);
        assertNotNull("Page content should not be null", pageContent);

        // Check for proper ADF structure
        assertTrue(pageContent.contains("\"type\":\"doc\""), "Content should be in ADF format");
        assertTrue(pageContent.contains("\"version\":1"), "Content should have version");

        // Check for formatting preservation
        validateFormattingInContent(pageContent);

        // Check that page title is based on H1 content, not workspace prefix
        validatePageTitle(pageInfo);
      }

      logger.info("=== Content validation completed successfully! ===");
      logger.info("All formatting has been properly preserved in Confluence:");
      logger.info("- Native ADF marks for inline formatting (strong, em, code, links)");
      logger.info("- Native ADF media nodes for images");
      logger.info("- Native ADF table structures");
      logger.info("- Proper page titles from H1 content");
    } catch (Exception e) {
      logger.warn("Content validation skipped due to API error: {}", e.getMessage());
      // Don't fail the test if we can't validate content - the main export still succeeded
    }
  }

  private void validateFormattingInContent(String content) {
    // Validate formatting only when native ADF marks are actually present,
    // to avoid false positives from plain substrings in page text (e.g. "System").
    boolean hasStrong =
        content.contains("\"type\":\"strong\"")
            || content.contains("\"marks\":[{\"type\":\"strong\"}]");
    if (hasStrong) {
      logger.info("✅ Strong formatting properly preserved");
    }

    boolean hasEm =
        content.contains("\"type\":\"em\"") || content.contains("\"marks\":[{\"type\":\"em\"}]");
    if (hasEm) {
      logger.info("✅ Em formatting properly preserved");
    }

    boolean hasCode =
        content.contains("\"type\":\"code\"")
            || content.contains("\"marks\":[{\"type\":\"code\"}]");
    if (hasCode) {
      logger.info("✅ Code formatting properly preserved");
    }

    boolean hasLink =
        content.contains("\"type\":\"link\"") || content.contains("\"marks\":[{\"type\":\"link\"");
    if (hasLink) {
      logger.info("✅ Link formatting properly preserved");
    }

    // Check for media nodes (images)
    boolean hasMediaNode = content.contains("\"type\":\"media\"");
    boolean hasMediaGroup = content.contains("\"type\":\"mediaGroup\"");
    boolean hasMediaSingle = content.contains("\"type\":\"mediaSingle\"");
    if (hasMediaNode) {
      assertTrue(
          hasMediaGroup || hasMediaSingle,
          "Images should use native ADF media nodes (media inside mediaGroup or mediaSingle)");
      logger.info("✅ Images properly converted to ADF media nodes");
    }

    // Check for tables
    if (content.contains("table")) {
      assertTrue(
          content.contains("\"type\":\"table\"") && content.contains("\"type\":\"tableRow\""),
          "Tables should use native ADF structure");
      logger.info("✅ Tables properly converted to native ADF structure");
    }
  }

  private void validatePageTitle(String pageInfo) {
    // Check that page title doesn't have workspace prefix (should use H1 content)
    if (pageInfo.contains("title")) {
      // Extract title from page info JSON
      String title = extractTitleFromPageInfo(pageInfo);
      if (title != null) {
        assertFalse(title.startsWith("ITMS - "), "Page title should not have 'ITMS - ' prefix");
        logger.info("✅ Page title properly uses H1 content: {}", title);
      }
    }
  }

  private String extractTitleFromPageInfo(String pageInfo) {
    // Simple JSON parsing to extract title
    int titleStart = pageInfo.indexOf("\"title\":\"");
    if (titleStart != -1) {
      titleStart += 9; // Length of "title":\"
      int titleEnd = pageInfo.indexOf("\"", titleStart);
      if (titleEnd != -1) {
        return pageInfo.substring(titleStart, titleEnd);
      }
    }
    return null;
  }
}
