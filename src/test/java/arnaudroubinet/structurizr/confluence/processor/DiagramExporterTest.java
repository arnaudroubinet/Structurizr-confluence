package arnaudroubinet.structurizr.confluence.processor;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test for DiagramExporter functionality
 */
class DiagramExporterTest {
    private static final Logger logger = LoggerFactory.getLogger(DiagramExporterTest.class);
    
    @Test
    void testFromEnvironmentWithoutVariables() {
        
        // Si l'environnement de CI définit déjà les variables, ignorer ce test
        if (System.getenv("STRUCTURIZR_URL") != null ||
            System.getenv("STRUCTURIZR_USERNAME") != null ||
            System.getenv("STRUCTURIZR_PASSWORD") != null) {
            logger.info("STRUCTURIZR_* variables present in environment; skipping null expectation test");
            return;
        }

        // Test when environment variables are not set
        DiagramExporter exporter = DiagramExporter.fromEnvironment("123");
        
        assertNull(exporter, "DiagramExporter should be null when environment variables are not set");
        
        logger.info("✅ DiagramExporter correctly returns null when STRUCTURIZR_* variables are not configured");
    }
    
    @Test
    void testFromEnvironmentWithVariables() {
        
        // This test would require setting environment variables
        // In a real scenario, the CI/CD would set these
        
        // For now, just test the structure
        String workspaceId = "123";
        DiagramExporter exporter = new DiagramExporter(
            "https://structurizr.roubinet.fr", 
            "testuser", 
            "testpass", 
            workspaceId
        );
        
        assertNotNull(exporter, "DiagramExporter should not be null when properly constructed");
        assertNotNull(exporter.getOutputDirectory(), "Output directory should be configured");
        assertTrue(exporter.getOutputDirectory().toString().contains("target/diagrams"), 
            "Output directory should be in target/diagrams");
        
        logger.info("✅ DiagramExporter structure validated successfully");
    }
    
    @Test 
    void testOutputDirectoryCreation() {
        
        DiagramExporter exporter = new DiagramExporter(
            "https://structurizr.roubinet.fr",
            "testuser", 
            "testpass",
            "123"
        );
        
        // Test cleanup functionality
        assertDoesNotThrow(() -> {
            exporter.cleanup();
            logger.info("Cleanup completed without errors");
        });
        
        logger.info("✅ Output directory handling validated");
    }
}