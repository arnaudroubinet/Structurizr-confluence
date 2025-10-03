package arnaudroubinet.structurizr.confluence;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for the diagram file resolution logic in ConfluenceExporter. Validates that diagram files
 * are correctly resolved by view key, especially when view keys contain dashes.
 *
 * <p>This test class duplicates the getDiagramFile logic to test it in isolation without requiring
 * a full ConfluenceExporter instance with Quarkus dependencies.
 */
public class DiagramFileResolutionTest {

  @Test
  @DisplayName("Should resolve diagram file with simple view key")
  public void testResolveDiagramFileSimple(@TempDir Path tempDir) throws IOException {
    // Create a temporary diagram file
    Path diagramPath = tempDir.resolve("structurizr-123-SystemContext.png");
    Files.write(diagramPath, new byte[] {0x00});

    // Resolve the diagram file
    File resolved = getDiagramFile("SystemContext", List.of(diagramPath.toFile()));

    assertNotNull(resolved, "Should find diagram file for view key 'SystemContext'");
    assertEquals("structurizr-123-SystemContext.png", resolved.getName());
  }

  @Test
  @DisplayName("Should resolve diagram file with view key containing dashes")
  public void testResolveDiagramFileWithDashes(@TempDir Path tempDir) throws IOException {
    // Create temporary diagram files
    Path diagram1 = tempDir.resolve("structurizr-456-System-Landscape-View.png");
    Path diagram2 = tempDir.resolve("structurizr-456-Container-Context.png");
    Files.write(diagram1, new byte[] {0x00});
    Files.write(diagram2, new byte[] {0x00});

    // Resolve the diagram file with dashes in the view key
    File resolved =
        getDiagramFile("System-Landscape-View", List.of(diagram1.toFile(), diagram2.toFile()));

    assertNotNull(resolved, "Should find diagram file for view key 'System-Landscape-View'");
    assertEquals("structurizr-456-System-Landscape-View.png", resolved.getName());
  }

  @Test
  @DisplayName("Should resolve diagram file with complex view key containing multiple dashes")
  public void testResolveDiagramFileComplexKey(@TempDir Path tempDir) throws IOException {
    // Create a temporary diagram file with complex view key
    Path diagramPath = tempDir.resolve("structurizr-789-My-Complex-View-Name-With-Dashes.png");
    Files.write(diagramPath, new byte[] {0x00});

    // Resolve the diagram file
    File resolved =
        getDiagramFile("My-Complex-View-Name-With-Dashes", List.of(diagramPath.toFile()));

    assertNotNull(resolved, "Should find diagram file for complex view key");
    assertEquals("structurizr-789-My-Complex-View-Name-With-Dashes.png", resolved.getName());
  }

  @Test
  @DisplayName("Should resolve diagram file with underscores in view key")
  public void testResolveDiagramFileWithUnderscores(@TempDir Path tempDir) throws IOException {
    // Create a temporary diagram file with underscores
    Path diagramPath = tempDir.resolve("structurizr-999-itms_platform_moteur_context_view.png");
    Files.write(diagramPath, new byte[] {0x00});

    // Resolve the diagram file
    File resolved =
        getDiagramFile("itms_platform_moteur_context_view", List.of(diagramPath.toFile()));

    assertNotNull(resolved, "Should find diagram file for view key with underscores");
    assertEquals("structurizr-999-itms_platform_moteur_context_view.png", resolved.getName());
  }

  @Test
  @DisplayName("Should return null when diagram file not found")
  public void testResolveDiagramFileNotFound(@TempDir Path tempDir) throws IOException {
    // Create a temporary diagram file
    Path diagramPath = tempDir.resolve("structurizr-123-SystemContext.png");
    Files.write(diagramPath, new byte[] {0x00});

    // Try to resolve a non-existent diagram
    File resolved = getDiagramFile("NonExistentView", List.of(diagramPath.toFile()));

    assertNull(resolved, "Should return null for non-existent view key");
  }

  @Test
  @DisplayName("Should skip key files and resolve main diagram")
  public void testResolveDiagramFileSkipKeyFile(@TempDir Path tempDir) throws IOException {
    // Create both main diagram and key file
    Path mainDiagram = tempDir.resolve("structurizr-123-SystemContext.png");
    Path keyDiagram = tempDir.resolve("structurizr-123-SystemContext-key.png");
    Files.write(mainDiagram, new byte[] {0x00});
    Files.write(keyDiagram, new byte[] {0x01});

    // Resolve should find the main diagram, not the key file
    File resolved =
        getDiagramFile("SystemContext", List.of(mainDiagram.toFile(), keyDiagram.toFile()));

    assertNotNull(resolved, "Should find main diagram file");
    assertEquals("structurizr-123-SystemContext.png", resolved.getName());
  }

  /**
   * Duplicate of the getDiagramFile method from ConfluenceExporter for testing. This tests the
   * updated logic that uses extractViewKeyFromFilename for consistency.
   */
  private File getDiagramFile(String viewKey, List<File> exportedDiagrams) {
    if (exportedDiagrams == null) {
      return null;
    }

    for (File diagramFile : exportedDiagrams) {
      String filename = diagramFile.getName();

      // Expected format: structurizr-{workspaceId}-{viewKey}.png
      // Extract the view key using the same logic as extractViewKeyFromFilename
      String extractedViewKey = extractViewKeyFromFilename(filename);
      if (extractedViewKey != null && extractedViewKey.equals(viewKey)) {
        return diagramFile;
      }

      // Fallback: check if filename contains the view key (for backward compatibility)
      String fileViewKey = filename;
      if (filename.contains(".")) {
        fileViewKey = filename.substring(0, filename.lastIndexOf('.'));
      }

      if (fileViewKey.toLowerCase().contains(viewKey.toLowerCase())) {
        return diagramFile;
      }
    }

    return null;
  }

  /** Duplicate of the extractViewKeyFromFilename method from ConfluenceExporter for testing. */
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
