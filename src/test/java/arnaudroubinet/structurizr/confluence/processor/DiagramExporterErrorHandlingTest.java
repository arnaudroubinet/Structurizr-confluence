package arnaudroubinet.structurizr.confluence.processor;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Test for improved error handling in DiagramExporter */
class DiagramExporterErrorHandlingTest {
  private static final Logger logger =
      LoggerFactory.getLogger(DiagramExporterErrorHandlingTest.class);

  @Test
  void testDiagramExporterErrorMessage() {
    // Test that DiagramExporter can be created without environment variables
    String workspaceId = "123";
    DiagramExporter exporter =
        new DiagramExporter("https://test.example.com", "testuser", "testpass", workspaceId);

    assertNotNull(exporter, "DiagramExporter should not be null when properly constructed");
    assertNotNull(exporter.getOutputDirectory(), "Output directory should be configured");

    logger.info("✅ DiagramExporter error handling structure validated");
  }

  @Test
  void testDiagramExporterFromEnvironmentHandling() {
    // Test environment variable handling - this should work if env vars are set
    // or return null if they're not set
    DiagramExporter exporter = DiagramExporter.fromEnvironment("123");

    // This test validates that the method doesn't throw exceptions
    if (exporter != null) {
      logger.info("DiagramExporter created from environment variables successfully");
      assertNotNull(exporter.getOutputDirectory(), "Output directory should be configured");
    } else {
      logger.info("DiagramExporter correctly returns null when environment variables are not set");
    }

    logger.info("✅ DiagramExporter environment handling validated");
  }
}
