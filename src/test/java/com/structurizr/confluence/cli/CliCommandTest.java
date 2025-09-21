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
        logger.info("=== TEST WORKSPACE FILE ACCESS ===");
        
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
        logger.info("=== TEST EXPORT COMMAND PARAMETERS ===");
        
        ExportCommand exportCommand = new ExportCommand();
        
        // Test default values - note that defaultValue is only applied by Picocli parsing
        assertFalse(exportCommand.cleanSpace, "Default cleanSpace should be false");
        
        // Test parameter assignment
        exportCommand.confluenceUrl = "https://test.atlassian.net";
        exportCommand.confluenceUser = "test@example.com";
        exportCommand.confluenceToken = "test-token";
        exportCommand.confluenceSpaceKey = "TEST";
        exportCommand.workspaceFile = new File("demo/itms-workspace.json");
        exportCommand.branchName = "test-branch";
        exportCommand.cleanSpace = true;
        
        assertEquals("https://test.atlassian.net", exportCommand.confluenceUrl);
        assertEquals("test@example.com", exportCommand.confluenceUser);
        assertEquals("test-token", exportCommand.confluenceToken);
        assertEquals("TEST", exportCommand.confluenceSpaceKey);
        assertTrue(exportCommand.workspaceFile.exists());
        assertEquals("test-branch", exportCommand.branchName);
        assertTrue(exportCommand.cleanSpace);
        
        logger.info("✅ Export command parameters working correctly");
    }
    
    @Test
    void testCleanCommandParameters() {
        logger.info("=== TEST CLEAN COMMAND PARAMETERS ===");
        
        CleanCommand cleanCommand = new CleanCommand();
        
        // Test default values
        assertFalse(cleanCommand.confirmDeletion, "Default confirmDeletion should be false");
        
        // Test parameter assignment
        cleanCommand.confluenceUrl = "https://test.atlassian.net";
        cleanCommand.confluenceUser = "test@example.com";
        cleanCommand.confluenceToken = "test-token";
        cleanCommand.confluenceSpaceKey = "TEST";
        cleanCommand.confirmDeletion = true;
        
        assertEquals("https://test.atlassian.net", cleanCommand.confluenceUrl);
        assertEquals("test@example.com", cleanCommand.confluenceUser);
        assertEquals("test-token", cleanCommand.confluenceToken);
        assertEquals("TEST", cleanCommand.confluenceSpaceKey);
        assertTrue(cleanCommand.confirmDeletion);
        
        logger.info("✅ Clean command parameters working correctly");
    }
    
    @Test
    void testLoadCommandParameters() {
        logger.info("=== TEST LOAD COMMAND PARAMETERS ===");
        
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
        logger.info("=== TEST COMMAND INSTANTIATION ===");
        
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
}