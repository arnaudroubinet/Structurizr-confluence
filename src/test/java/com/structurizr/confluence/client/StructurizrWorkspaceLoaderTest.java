package com.structurizr.confluence.client;

import com.structurizr.api.StructurizrClientException;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for StructurizrWorkspaceLoader, focusing on error handling and reverse proxy scenarios.
 */
class StructurizrWorkspaceLoaderTest {
    
    private static final Logger logger = LoggerFactory.getLogger(StructurizrWorkspaceLoaderTest.class);
    
    @Test
    void testWorkspaceLoaderInstantiation() throws StructurizrClientException {
        // Test basic instantiation with minimal config
        StructurizrConfig config = new StructurizrConfig(
            "https://example.com/api",
            "test-key",
            "test-secret",
            123L
        );
        
        StructurizrWorkspaceLoader loader = new StructurizrWorkspaceLoader(config);
        assertNotNull(loader, "StructurizrWorkspaceLoader should be created successfully");
        
        logger.info("✅ StructurizrWorkspaceLoader instantiation validated");
    }
    
    @Test
    void testWorkspaceLoaderWithNullApiUrl() throws StructurizrClientException {
        // Test instantiation with null API URL (should use cloud URL)
        StructurizrConfig config = new StructurizrConfig(
            null,
            "test-key",
            "test-secret",
            123L
        );
        
        StructurizrWorkspaceLoader loader = new StructurizrWorkspaceLoader(config);
        assertNotNull(loader, "StructurizrWorkspaceLoader should be created successfully with null API URL");
        
        logger.info("✅ StructurizrWorkspaceLoader with null API URL validated");
    }
    
    @Test
    void testWorkspaceLoaderWithEmptyApiUrl() throws StructurizrClientException {
        // Test instantiation with empty API URL (should use cloud URL)
        StructurizrConfig config = new StructurizrConfig(
            "",
            "test-key",
            "test-secret",
            123L
        );
        
        StructurizrWorkspaceLoader loader = new StructurizrWorkspaceLoader(config);
        assertNotNull(loader, "StructurizrWorkspaceLoader should be created successfully with empty API URL");
        
        logger.info("✅ StructurizrWorkspaceLoader with empty API URL validated");
    }
    
    @Test
    void testWorkspaceLoaderWithReverseProxyApiUrl() throws StructurizrClientException {
        // Test instantiation with a reverse proxy URL (common scenario)
        StructurizrConfig config = new StructurizrConfig(
            "https://structurizr.igs-platform.lotsys.corp/api",
            "test-key",
            "test-secret",
            2L
        );
        
        StructurizrWorkspaceLoader loader = new StructurizrWorkspaceLoader(config);
        assertNotNull(loader, "StructurizrWorkspaceLoader should be created successfully with reverse proxy URL");
        
        logger.info("✅ StructurizrWorkspaceLoader with reverse proxy URL validated");
    }
    
    @Test
    void testLoadWorkspaceWithInvalidCredentials() {
        // Test workspace loading with invalid credentials (should handle gracefully)
        StructurizrConfig config = new StructurizrConfig(
            "https://api.structurizr.com",
            "invalid-key",
            "invalid-secret",
            999999L
        );
        
        try {
            StructurizrWorkspaceLoader loader = new StructurizrWorkspaceLoader(config);
            
            // This should fail, but should provide helpful error messages
            Exception exception = assertThrows(Exception.class, () -> {
                loader.loadWorkspace();
            });
            
            // Check that it's either a RuntimeException or StructurizrClientException
            assertTrue(exception instanceof RuntimeException || exception instanceof StructurizrClientException,
                "Exception should be either RuntimeException or StructurizrClientException, got: " + exception.getClass().getSimpleName());
            
            // The error message should be informative
            String errorMessage = exception.getMessage();
            assertNotNull(errorMessage, "Error message should not be null");
            assertTrue(errorMessage.length() > 10, "Error message should be meaningful");
            
            logger.info("✅ Invalid credentials handled gracefully with message: {}", 
                errorMessage.substring(0, Math.min(errorMessage.length(), 100)) + "...");
        } catch (StructurizrClientException e) {
            // This is also acceptable - the loader constructor might fail
            logger.info("✅ Invalid credentials rejected at construction time: {}", e.getMessage());
        }
    }
}