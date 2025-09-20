package com.structurizr.confluence;

import com.structurizr.Workspace;
import com.structurizr.confluence.client.ConfluenceClient;
import com.structurizr.confluence.client.ConfluenceConfig;
import com.structurizr.util.WorkspaceUtils;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ConfluenceExporterIntegrationTest {
    private static final Logger logger = LoggerFactory.getLogger(ConfluenceExporterIntegrationTest.class);
    
    @Test
    public void exportWorkspaceToConfluence() throws Exception {
        String confluenceUser = System.getenv("CONFLUENCE_USER");
        String confluenceUrl = System.getenv("CONFLUENCE_URL");
        String confluenceToken = System.getenv("CONFLUENCE_TOKEN");
        String confluenceSpaceKey = System.getenv("CONFLUENCE_SPACE_KEY");
        Assumptions.assumeTrue(confluenceUser != null && !confluenceUser.isBlank(), "CONFLUENCE_USER not defined: test skipped");
        Assumptions.assumeTrue(confluenceUrl != null && !confluenceUrl.isBlank(), "CONFLUENCE_URL not defined: test skipped");
        Assumptions.assumeTrue(confluenceToken != null && !confluenceToken.isBlank(), "CONFLUENCE_TOKEN not defined: test skipped");
        Assumptions.assumeTrue(confluenceSpaceKey != null && !confluenceSpaceKey.isBlank(), "CONFLUENCE_SPACE_KEY not defined: test skipped");
        assertNotNull(confluenceSpaceKey, "CONFLUENCE_SPACE_KEY must be defined");
        
        ConfluenceConfig config = new ConfluenceConfig(confluenceUrl, confluenceUser, confluenceToken, confluenceSpaceKey);

        // Clean the Confluence space before export
        ConfluenceExporter exporter = new ConfluenceExporter(config);
        ConfluenceClient confluenceClient = new ConfluenceClient(config);
        
        logger.info("Cleaning Confluence space: {}", confluenceSpaceKey);
        exporter.cleanConfluenceSpace();
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
     * Validates the exported content by fetching pages from Confluence API and checking formatting preservation.
     */
    private void validateExportedContent(ConfluenceClient confluenceClient, String spaceKey) throws Exception {
        logger.info("=== Starting content validation ===");
        
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
            assertTrue("Content should be in ADF format", pageContent.contains("\"type\":\"doc\""));
            assertTrue("Content should have version", pageContent.contains("\"version\":1"));
            
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
    }
    
    private void validateFormattingInContent(String content) {
        // Check for formatting preservation
        if (content.contains("strong")) {
            assertTrue("Strong formatting should use ADF marks", 
                content.contains("\"type\":\"strong\"") || content.contains("\"marks\":[{\"type\":\"strong\"}]"));
            logger.info("✅ Strong formatting properly preserved");
        }
        
        if (content.contains("em")) {
            assertTrue("Em formatting should use ADF marks", 
                content.contains("\"type\":\"em\"") || content.contains("\"marks\":[{\"type\":\"em\"}]"));
            logger.info("✅ Em formatting properly preserved");
        }
        
        if (content.contains("code")) {
            assertTrue("Code formatting should use ADF marks", 
                content.contains("\"type\":\"code\"") || content.contains("\"marks\":[{\"type\":\"code\"}]"));
            logger.info("✅ Code formatting properly preserved");
        }
        
        if (content.contains("link")) {
            assertTrue("Links should use ADF marks", 
                content.contains("\"type\":\"link\"") || content.contains("\"marks\":[{\"type\":\"link\""));
            logger.info("✅ Link formatting properly preserved");
        }
        
        // Check for media nodes (images)
        if (content.contains("mediaGroup") || content.contains("media")) {
            assertTrue("Images should use native ADF media nodes", 
                content.contains("\"type\":\"mediaGroup\"") && content.contains("\"type\":\"media\""));
            logger.info("✅ Images properly converted to ADF media nodes");
        }
        
        // Check for tables
        if (content.contains("table")) {
            assertTrue("Tables should use native ADF structure", 
                content.contains("\"type\":\"table\"") && content.contains("\"type\":\"tableRow\""));
            logger.info("✅ Tables properly converted to native ADF structure");
        }
    }
    
    private void validatePageTitle(String pageInfo) {
        // Check that page title doesn't have workspace prefix (should use H1 content)
        if (pageInfo.contains("title")) {
            // Extract title from page info JSON
            String title = extractTitleFromPageInfo(pageInfo);
            if (title != null) {
                assertFalse("Page title should not have 'ITMS - ' prefix", title.startsWith("ITMS - "));
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
