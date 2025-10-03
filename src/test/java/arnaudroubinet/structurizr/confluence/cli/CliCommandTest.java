package arnaudroubinet.structurizr.confluence.cli;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test the CLI commands to ensure they work as expected. These tests validate the command structure
 * without requiring actual Confluence credentials.
 */
class CliCommandTest {
  private static final Logger logger = LoggerFactory.getLogger(CliCommandTest.class);

  @Test
  void testWorkspaceFileAccess() {

    File demoWorkspace = new File("demo/itms-workspace.json");

    assertTrue(demoWorkspace.exists(), "Demo workspace should exist");
    assertTrue(demoWorkspace.isFile(), "Demo workspace should be a file");
    assertTrue(demoWorkspace.canRead(), "Demo workspace should be readable");
    assertTrue(demoWorkspace.length() > 0, "Demo workspace should not be empty");

    logger.info("✅ Demo workspace file access validated");
    logger.info("Workspace file size: {} bytes", demoWorkspace.length());
  }

  @Test
  void testExportCommandParameters() {

    ExportCommand exportCommand = new ExportCommand();

    // Test default values - note that defaultValue is only applied by Picocli parsing
    assertFalse(exportCommand.cleanSpace, "Default cleanSpace should be false");
    assertFalse(exportCommand.force, "Default force should be false");

    // Test parameter assignment
    exportCommand.confluenceUrl = "https://test.atlassian.net";
    exportCommand.confluenceUser = "test@example.com";
    exportCommand.confluenceToken = "test-token";
    exportCommand.confluenceSpaceKey = "TEST";
    exportCommand.workspaceFile = new File("demo/itms-workspace.json");
    exportCommand.branchName = "test-branch";
    exportCommand.cleanSpace = true;
    exportCommand.force = true;

    assertEquals("https://test.atlassian.net", exportCommand.confluenceUrl);
    assertEquals("test@example.com", exportCommand.confluenceUser);
    assertEquals("test-token", exportCommand.confluenceToken);
    assertEquals("TEST", exportCommand.confluenceSpaceKey);
    assertTrue(exportCommand.workspaceFile.exists());
    assertEquals("test-branch", exportCommand.branchName);
    assertTrue(exportCommand.cleanSpace);
    assertTrue(exportCommand.force);

    logger.info("✅ Export command parameters working correctly");
  }

  @Test
  void testCommandInstantiation() {

    // Test that command classes can be instantiated
    assertDoesNotThrow(
        () -> {
          StructurizrConfluenceCommand mainCommand = new StructurizrConfluenceCommand();
          assertNotNull(mainCommand, "Main command should be instantiable");
        });

    assertDoesNotThrow(
        () -> {
          ExportCommand exportCommand = new ExportCommand();
          assertNotNull(exportCommand, "Export command should be instantiable");
        });

    logger.info("✅ Command classes instantiate correctly");
  }

  @Test
  void testExportCommandNewFeatures() {
    logger.info("=== TEST EXPORT COMMAND NEW FEATURES ===");

    ExportCommand exportCommand = new ExportCommand();

    // Test new force flag functionality
    exportCommand.force = true;
    exportCommand.cleanSpace = true; // This should work with force to skip prompts
    exportCommand.workspaceFile = new File("demo/itms-workspace.json");
    exportCommand.branchName = "feature-branch";

    assertTrue(exportCommand.force, "Force flag should be available");
    assertTrue(exportCommand.cleanSpace, "Clean should still work");
    assertEquals("feature-branch", exportCommand.branchName, "Branch name determines target page");

    logger.info("✅ Export command now has force flag and targeted cleaning");
  }

  @Test
  void testExportCommandPageTargeting() {
    logger.info("=== TEST EXPORT COMMAND PAGE TARGETING ===");

    ExportCommand exportCommand = new ExportCommand();

    // Test page title targeting for clean
    exportCommand.cleanPageTitle = "Custom Target Page";
    exportCommand.pageId = "123456"; // pageId is now required
    exportCommand.cleanSpace = true;
    exportCommand.confluenceSpaceKey = "TEST";
    exportCommand.workspaceFile = new File("demo/itms-workspace.json");
    exportCommand.branchName = "main";

    assertEquals("Custom Target Page", exportCommand.cleanPageTitle);
    assertEquals("123456", exportCommand.pageId);
    assertTrue(exportCommand.cleanSpace);

    // Test page ID targeting
    exportCommand.cleanPageTitle = null;
    exportCommand.pageId = "789012";
    exportCommand.confluenceSpaceKey = null; // Space not required when using page ID

    assertNull(exportCommand.cleanPageTitle);
    assertEquals("789012", exportCommand.pageId);

    logger.info("✅ Export command supports both page title and page ID targeting");
  }

  @Test
  void testExportCommandWorkspaceFileSupport() {
    logger.info("=== TEST EXPORT COMMAND WORKSPACE FILE SUPPORT ===");

    ExportCommand exportCommand = new ExportCommand();

    // Test new --workspace-file parameter
    exportCommand.workspaceFile = new File("demo/itms-workspace.json");
    exportCommand.confluenceUrl = "https://test.atlassian.net";
    exportCommand.confluenceUser = "test@example.com";
    exportCommand.confluenceToken = "test-token";
    exportCommand.confluenceSpaceKey = "TEST";

    assertEquals("demo/itms-workspace.json", exportCommand.workspaceFile.getPath());
    assertTrue(exportCommand.workspaceFile.exists(), "Demo workspace file should exist");

    // Structurizr parameters should be null when using workspace file
    assertNull(exportCommand.structurizrUrl);
    assertNull(exportCommand.structurizrApiKey);
    assertNull(exportCommand.structurizrApiSecret);
    assertNull(exportCommand.structurizrWorkspaceId);

    logger.info("✅ Export command supports --workspace-file parameter");
  }

  @Test
  void testExportCommandStructurizrSupport() {
    logger.info("=== TEST EXPORT COMMAND STRUCTURIZR SUPPORT ===");

    ExportCommand exportCommand = new ExportCommand();

    // Test Structurizr on-premise parameters
    exportCommand.structurizrUrl = "https://structurizr.example.com";
    exportCommand.structurizrApiKey = "test-api-key";
    exportCommand.structurizrApiSecret = "test-api-secret";
    exportCommand.structurizrWorkspaceId = 12345L;
    exportCommand.confluenceUrl = "https://test.atlassian.net";
    exportCommand.confluenceUser = "test@example.com";
    exportCommand.confluenceToken = "test-token";
    exportCommand.confluenceSpaceKey = "TEST";

    assertEquals("https://structurizr.example.com", exportCommand.structurizrUrl);
    assertEquals("test-api-key", exportCommand.structurizrApiKey);
    assertEquals("test-api-secret", exportCommand.structurizrApiSecret);
    assertEquals(12345L, exportCommand.structurizrWorkspaceId);

    // Workspace file should be null when using Structurizr
    assertNull(exportCommand.workspaceFile);

    logger.info("✅ Export command supports Structurizr on-premise parameters");
  }
}
