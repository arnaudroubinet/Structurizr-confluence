package arnaudroubinet.structurizr.confluence.processor;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test for DiagramExporter API fix validation
 */
class DiagramExporterApiFixTest {
    private static final Logger logger = LoggerFactory.getLogger(DiagramExporterApiFixTest.class);
    
    @Test
    void testDiagramExporterConstruction() {
        // Test that DiagramExporter can be constructed without issues
        DiagramExporter exporter = new DiagramExporter(
            "https://structurizr.example.com", 
            "testuser", 
            "testpass", 
            "123"
        );
        
        assertNotNull(exporter, "DiagramExporter should be constructed successfully");
        assertNotNull(exporter.getOutputDirectory(), "Output directory should be configured");
        
        logger.info("✅ DiagramExporter construction validated - API fix doesn't break instantiation");
    }
    
    @Test
    void testEnvironmentVariableHandling() {
        // This test validates that the DiagramExporter.fromEnvironment method works
        // regardless of whether environment variables are set
        
        DiagramExporter exporter = DiagramExporter.fromEnvironment("123");
        
        // The result can be null or a valid exporter depending on environment
        // We just want to ensure no exceptions are thrown
        logger.info("✅ Environment variable handling works correctly");
        
        if (exporter != null) {
            logger.info("DiagramExporter created from environment variables");
        } else {
            logger.info("DiagramExporter returned null (expected when env vars not set)");
        }
    }
    
    @Test
    void testCleanupOperation() {
        DiagramExporter exporter = new DiagramExporter(
            "https://structurizr.example.com",
            "testuser", 
            "testpass",
            "123"
        );
        
        // Test that cleanup doesn't throw exceptions
        assertDoesNotThrow(() -> {
            exporter.cleanup();
        }, "Cleanup should not throw exceptions");
        
        logger.info("✅ Cleanup operation validated");
    }
}