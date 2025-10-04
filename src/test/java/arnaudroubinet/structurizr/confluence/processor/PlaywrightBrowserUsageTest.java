package arnaudroubinet.structurizr.confluence.processor;

import static org.junit.jupiter.api.Assertions.*;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test to verify that the application only uses Chromium browser from Playwright. This ensures we
 * only need to install chromium, not all browsers (firefox, webkit).
 *
 * <p>Related issue: "Playwright load many browsers, why? I only need chromium when loading my
 * schemas."
 */
class PlaywrightBrowserUsageTest {
  private static final Logger logger = LoggerFactory.getLogger(PlaywrightBrowserUsageTest.class);

  @Test
  void testOnlyChromiumIsUsedInDiagramExporter() throws IOException {
    logger.info("=== Verifying Playwright browser usage ===");

    Path sourceFile =
        Paths.get(
            "src/main/java/arnaudroubinet/structurizr/confluence/processor/DiagramExporter.java");
    assertTrue(sourceFile.toFile().exists(), "DiagramExporter.java should exist");

    StringBuilder content = new StringBuilder();
    try (BufferedReader reader = new BufferedReader(new FileReader(sourceFile.toFile()))) {
      String line;
      while ((line = reader.readLine()) != null) {
        content.append(line).append("\n");
      }
    }

    String sourceCode = content.toString();

    assertTrue(sourceCode.contains(".chromium()"), "DiagramExporter should use .chromium()");

    assertFalse(
        sourceCode.contains(".firefox()"),
        "DiagramExporter should NOT use .firefox() - only chromium is needed");

    assertFalse(
        sourceCode.contains(".webkit()"),
        "DiagramExporter should NOT use .webkit() - only chromium is needed");

    logger.info("✅ Verified: Only Chromium browser is used");
    logger.info(
        "   This means we can install with: mvn exec:java -D exec.mainClass=com.microsoft.playwright.CLI -D exec.args=\"install --with-deps chromium\"");
    logger.info("   Instead of installing all browsers (chromium, firefox, webkit)");
  }

  @Test
  void testGitHubWorkflowUsesChromiumOnly() throws IOException {
    logger.info("=== Verifying GitHub Actions workflow ===");

    Path workflowFile = Paths.get(".github/workflows/ci.yml");
    assertTrue(workflowFile.toFile().exists(), "ci.yml should exist");

    StringBuilder content = new StringBuilder();
    try (BufferedReader reader = new BufferedReader(new FileReader(workflowFile.toFile()))) {
      String line;
      while ((line = reader.readLine()) != null) {
        content.append(line).append("\n");
      }
    }

    String workflowContent = content.toString();

    assertTrue(
        workflowContent.contains("install --with-deps chromium"),
        "GitHub Actions workflow should install only chromium browser");

    assertFalse(
        workflowContent.matches(".*install --with-deps\"\\s*$.*"),
        "GitHub Actions workflow should not install all browsers by default");

    logger.info("✅ Verified: GitHub Actions workflow installs only Chromium");
  }
}
