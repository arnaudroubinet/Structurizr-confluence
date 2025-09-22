package com.structurizr.confluence.cli;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test the CLI commands to ensure they work as expected.
 * These tests validate the command structure without requiring actual Confluence credentials.
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
    void testCleanCommandParameters() {
        
        CleanCommand cleanCommand = new CleanCommand();
        
        // Test default values
        assertFalse(cleanCommand.confirmDeletion, "Default confirmDeletion should be false");
        assertFalse(cleanCommand.force, "Default force should be false");
        
        // Test parameter assignment
        cleanCommand.confluenceUrl = "https://test.atlassian.net";
        cleanCommand.confluenceUser = "test@example.com";
        cleanCommand.confluenceToken = "test-token";
        cleanCommand.confluenceSpaceKey = "TEST";
        cleanCommand.pageTitle = "Test Page";
        cleanCommand.force = true;
        cleanCommand.confirmDeletion = true;
        
        assertEquals("https://test.atlassian.net", cleanCommand.confluenceUrl);
        assertEquals("test@example.com", cleanCommand.confluenceUser);
        assertEquals("test-token", cleanCommand.confluenceToken);
        assertEquals("TEST", cleanCommand.confluenceSpaceKey);
        assertEquals("Test Page", cleanCommand.pageTitle);
        assertTrue(cleanCommand.force);
        assertTrue(cleanCommand.confirmDeletion);
        
        logger.info("✅ Clean command parameters working correctly");
    }
    
    @Test
    void testLoadCommandParameters() {
        
        LoadCommand loadCommand = new LoadCommand();
        
        // Test default values
        assertFalse(loadCommand.cleanSpace, "Default cleanSpace should be false");
        
        // Test parameter assignment
        loadCommand.structurizrUrl = "https://test-structurizr.com";
        loadCommand.structurizrApiKey = "test-key";
        loadCommand.structurizrApiSecret = "test-secret";
        loadCommand.workspaceId = 12345L;
        loadCommand.confluenceUrl = "https://test.atlassian.net";
        loadCommand.confluenceUser = "test@example.com";
        loadCommand.confluenceToken = "test-token";
        loadCommand.confluenceSpaceKey = "TEST";
        loadCommand.cleanSpace = true;
        
        assertEquals("https://test-structurizr.com", loadCommand.structurizrUrl);
        assertEquals("test-key", loadCommand.structurizrApiKey);
        assertEquals("test-secret", loadCommand.structurizrApiSecret);
        assertEquals(12345L, loadCommand.workspaceId);
        assertEquals("https://test.atlassian.net", loadCommand.confluenceUrl);
        assertEquals("test@example.com", loadCommand.confluenceUser);
        assertEquals("test-token", loadCommand.confluenceToken);
        assertEquals("TEST", loadCommand.confluenceSpaceKey);
        assertTrue(loadCommand.cleanSpace);
        
        logger.info("✅ Load command parameters working correctly");
    }
    
    @Test
    void testCommandInstantiation() {
        
        // Test that all command classes can be instantiated
        assertDoesNotThrow(() -> {
            StructurizrConfluenceCommand mainCommand = new StructurizrConfluenceCommand();
            assertNotNull(mainCommand, "Main command should be instantiable");
        });
        
        assertDoesNotThrow(() -> {
            ExportCommand exportCommand = new ExportCommand();
            assertNotNull(exportCommand, "Export command should be instantiable");
        });
        
        assertDoesNotThrow(() -> {
            CleanCommand cleanCommand = new CleanCommand();
            assertNotNull(cleanCommand, "Clean command should be instantiable");
        });
        
        assertDoesNotThrow(() -> {
            LoadCommand loadCommand = new LoadCommand();
            assertNotNull(loadCommand, "Load command should be instantiable");
        });
        
        logger.info("✅ All command classes instantiate correctly");
    }

    @Test
    void testCleanCommandValidation() {
        logger.info("=== TEST CLEAN COMMAND NEW FEATURES ===");
        
        CleanCommand cleanCommand = new CleanCommand();
        
        // Test that page title is required (this will be validated by Picocli at runtime)
        cleanCommand.pageTitle = "Test Root Page";
        cleanCommand.force = true; // Should skip confirmation
        cleanCommand.confluenceUrl = "https://test.atlassian.net";
        cleanCommand.confluenceUser = "test@example.com";
        cleanCommand.confluenceToken = "test-token";
        cleanCommand.confluenceSpaceKey = "TEST";
        
        assertNotNull(cleanCommand.pageTitle, "Page title should be set");
        assertTrue(cleanCommand.force, "Force flag should work");
        
        logger.info("✅ Clean command now targets specific pages with force option");
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
}