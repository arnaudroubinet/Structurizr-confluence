package arnaudroubinet.structurizr.confluence;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests for the extractViewKeyFromFilename logic in ConfluenceExporter. This test validates the
 * filename parsing logic without requiring a full ConfluenceExporter instance.
 */
public class ViewKeyExtractionTest {

  @Test
  @DisplayName("Should extract view key from standard diagram filename")
  public void testExtractViewKeyStandard() {
    String viewKey = extractViewKeyFromFilename("structurizr-123-SystemContext.png");
    assertEquals("SystemContext", viewKey);
  }

  @Test
  @DisplayName("Should extract view key from key diagram filename")
  public void testExtractViewKeyFromKeyFile() {
    String viewKey = extractViewKeyFromFilename("structurizr-123-SystemContext-key.png");
    assertEquals("SystemContext", viewKey);
  }

  @Test
  @DisplayName("Should extract view key with dashes in name")
  public void testExtractViewKeyWithDashes() {
    String viewKey = extractViewKeyFromFilename("structurizr-456-System-Landscape-View.png");
    assertEquals("System-Landscape-View", viewKey);
  }

  @Test
  @DisplayName("Should return null for invalid filename")
  public void testExtractViewKeyInvalid() {
    String viewKey = extractViewKeyFromFilename("invalid-filename.png");
    assertNull(viewKey);
  }

  @Test
  @DisplayName("Should return null for null filename")
  public void testExtractViewKeyNull() {
    String viewKey = extractViewKeyFromFilename(null);
    assertNull(viewKey);
  }

  @Test
  @DisplayName("Should return null for filename without extension")
  public void testExtractViewKeyNoExtension() {
    String viewKey = extractViewKeyFromFilename("structurizr-123-SystemContext");
    assertNull(viewKey);
  }

  @Test
  @DisplayName("Should extract complex view key with multiple dashes")
  public void testExtractComplexViewKey() {
    String viewKey =
        extractViewKeyFromFilename("structurizr-789-My-Complex-View-Name-With-Dashes.png");
    assertEquals("My-Complex-View-Name-With-Dashes", viewKey);
  }

  // Duplicate of the extractViewKeyFromFilename method from ConfluenceExporter for testing
  private String extractViewKeyFromFilename(String filename) {
    if (filename == null || !filename.contains("-") || !filename.contains(".")) {
      return null;
    }

    // Remove extension
    String nameWithoutExt = filename.substring(0, filename.lastIndexOf('.'));

    // Remove "-key" suffix if present
    if (nameWithoutExt.endsWith("-key")) {
      nameWithoutExt = nameWithoutExt.substring(0, nameWithoutExt.length() - 4);
    }

    // Expected format: structurizr-{workspaceId}-{viewKey}
    // Find the second dash (after workspaceId)
    int firstDash = nameWithoutExt.indexOf('-');
    if (firstDash < 0) {
      return null;
    }

    int secondDash = nameWithoutExt.indexOf('-', firstDash + 1);
    if (secondDash < 0) {
      return null;
    }

    // Everything after the second dash is the view key
    return nameWithoutExt.substring(secondDash + 1);
  }
}
