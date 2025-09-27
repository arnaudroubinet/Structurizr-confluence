package com.structurizr.confluence;

import com.structurizr.confluence.client.ConfluenceClient;
import com.structurizr.confluence.client.ConfluenceConfig;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;

import java.io.File;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Docker integration test that validates the complete containerized CLI workflow:
 * 1. Builds the Docker image
 * 2. Creates a new test page in Confluence using environment variables
 * 3. Runs the export command from the Docker image using the new page ID
 * 4. Verifies the generated content has proper ADF structure
 */
@Testcontainers
public class DockerIntegrationTest {
    private static final Logger logger = LoggerFactory.getLogger(DockerIntegrationTest.class);
    
    @Container
    static GenericContainer<?> confluenceCliContainer = new GenericContainer<>(
        new ImageFromDockerfile()
            .withFileFromPath(".", Path.of(".").toAbsolutePath())
            .withDockerfile(Path.of("Dockerfile"))
    )
    .withStartupTimeout(Duration.ofMinutes(10))
    .withCommand("tail", "-f", "/dev/null") // Keep container running
    .withWorkingDirectory("/opt/structurizr");

    @Test
    public void testDockerExportWorkflow() throws Exception {
        // Check environment variables
        String confluenceUser = System.getenv("CONFLUENCE_USER");
        String confluenceUrl = System.getenv("CONFLUENCE_URL");
        String confluenceToken = System.getenv("CONFLUENCE_TOKEN");
        String confluenceSpaceKey = System.getenv("CONFLUENCE_SPACE_KEY");
        
        Assumptions.assumeTrue(confluenceUser != null && !confluenceUser.isBlank(), "CONFLUENCE_USER not defined: test skipped");
        Assumptions.assumeTrue(confluenceUrl != null && !confluenceUrl.isBlank(), "CONFLUENCE_URL not defined: test skipped");
        Assumptions.assumeTrue(confluenceToken != null && !confluenceToken.isBlank(), "CONFLUENCE_TOKEN not defined: test skipped");
        Assumptions.assumeTrue(confluenceSpaceKey != null && !confluenceSpaceKey.isBlank(), "CONFLUENCE_SPACE_KEY not defined: test skipped");

        logger.info("=== Starting Docker Integration Test ===");
        logger.info("Docker container started: {}", confluenceCliContainer.getContainerId());

        // Step 1: Create a test page in Confluence to use as target
        ConfluenceConfig config = new ConfluenceConfig(confluenceUrl, confluenceUser, confluenceToken, confluenceSpaceKey);
        ConfluenceClient confluenceClient = new ConfluenceClient(config);
        
        String testPageTitle = "Docker Test Page " + UUID.randomUUID().toString().substring(0, 8);
        logger.info("Creating test page: {}", testPageTitle);
        
        String testPageContent = "{\"version\":1,\"type\":\"doc\",\"content\":[{\"type\":\"paragraph\",\"content\":[{\"type\":\"text\",\"text\":\"This is a test page for Docker integration testing.\"}]}]}";
        String testPageId = confluenceClient.createOrUpdatePage(testPageTitle, testPageContent, null);
        assertNotNull(testPageId, "Test page should be created");
        logger.info("Created test page with ID: {}", testPageId);

        try {
            // Step 2: Run Docker export command 
            logger.info("=== Running Docker export ===");
            
            // Copy workspace file to container
            File workspaceFile = Path.of("demo/itms-workspace.json").toFile();
            assertTrue(workspaceFile.exists(), "Demo workspace file should exist");
            
            confluenceCliContainer.copyFileToContainer(
                MountableFile.forHostPath(workspaceFile.toPath()),
                "/opt/structurizr/" + workspaceFile.getName()
            );

            // Clean target page before export
            ConfluenceExporter cleaner = new ConfluenceExporter(config);
            cleaner.cleanPageTreeById(testPageId);

            // Run export command from Docker container
            var result = confluenceCliContainer.execInContainer("java", "-jar", "quarkus-app/quarkus-run.jar", "export",
                "--confluence-url", confluenceUrl,
                "--confluence-user", confluenceUser,
                "--confluence-token", confluenceToken, 
                "--confluence-space", confluenceSpaceKey,
                "--page-id", testPageId,
                "--workspace-file", workspaceFile.getName()
            );
            
            logger.info("Docker export exit code: {}", result.getExitCode());
            logger.info("Docker export stdout: {}", result.getStdout());
            if (result.getExitCode() != 0) {
                logger.error("Docker export stderr: {}", result.getStderr());
            }
            
            assertEquals(0, result.getExitCode(), "Docker export should succeed");

            // Wait for export to complete
            Thread.sleep(3000);

            // Step 3: Validate Docker export results
            logger.info("=== Validating Docker export results ===");
            
            // Get all pages in the space to see what was created
            List<String> allPageIds = confluenceClient.getSpacePageIds(confluenceSpaceKey);
            assertTrue(allPageIds.size() > 0, "Docker export should create pages");
            logger.info("Docker export created {} total pages in space", allPageIds.size());

            // Validate that content was properly exported
            boolean foundExportedContent = false;
            for (String pageId : allPageIds) {
                String pageContent = confluenceClient.getPageContent(pageId);
                assertNotNull(pageContent, "Page content should not be null");
                
                // Verify ADF structure
                assertTrue(pageContent.contains("\"type\":\"doc\""), "Content should be in ADF format");
                assertTrue(pageContent.contains("\"version\":1"), "Content should have ADF version");
                
                String pageInfo = confluenceClient.getPageInfo(pageId);
                logger.info("Validated Docker-exported page: {}", pageInfo);
                
                // Check if this looks like workspace-generated content
                if (pageContent.contains("ITMS") || pageContent.contains("architecture") || pageContent.contains("system")) {
                    foundExportedContent = true;
                }
            }
            
            assertTrue(foundExportedContent, "Should find workspace-related content in exported pages");
            
            logger.info("=== Docker Integration Test PASSED ===");
            logger.info("✅ Docker image built successfully");
            logger.info("✅ Docker export command executed successfully");
            logger.info("✅ Generated content has proper ADF structure");
            logger.info("✅ Found expected workspace content in exported pages");

        } finally {
            // Cleanup: Remove test page
            try {
                confluenceClient.deletePage(testPageId);
                logger.info("Cleaned up test page: {}", testPageId);
            } catch (Exception e) {
                logger.warn("Failed to cleanup test page {}: {}", testPageId, e.getMessage());
            }
        }
    }
}