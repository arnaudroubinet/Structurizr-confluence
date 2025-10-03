package arnaudroubinet.structurizr.confluence.processor;

import arnaudroubinet.structurizr.confluence.util.SslTrustUtils;
import com.microsoft.playwright.*;
import com.structurizr.Workspace;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Exports diagrams from a Structurizr workspace using Playwright Java. Downloads diagrams as local
 * files that can then be uploaded to Confluence.
 */
public class DiagramExporter {
  private static final Logger logger = LoggerFactory.getLogger(DiagramExporter.class);

  private final String structurizrUrl;
  private final String username;
  private final String password;
  private final String workspaceId;
  private final Path outputDirectory;
  private final int maxDurationSeconds;
  private final int viewportWidth;
  private final int viewportHeight;
  private final double deviceScaleFactor;
  private final double oversampleFactor;

  public DiagramExporter(
      String structurizrUrl, String username, String password, String workspaceId) {
    this.structurizrUrl = structurizrUrl;
    this.username = username;
    this.password = password;
    this.workspaceId = workspaceId;
    this.outputDirectory = Paths.get("target", "diagrams");
    // Overall timeout for the diagram export process. Defaults to 300 seconds.
    // Can be overridden with env var PLAYWRIGHT_MAX_DURATION_SECS to accommodate slower
    // environments
    this.maxDurationSeconds = parseIntEnv("PLAYWRIGHT_MAX_DURATION_SECS", 300);
    // Viewport / quality tuning (env configurable)
    this.viewportWidth = parseIntEnv("PLAYWRIGHT_VIEWPORT_WIDTH", 1920);
    this.viewportHeight = parseIntEnv("PLAYWRIGHT_VIEWPORT_HEIGHT", 1080);
    this.deviceScaleFactor =
        parseDoubleEnv("PLAYWRIGHT_DEVICE_SCALE", 2.0d); // higher => sharper rasterized SVG
    this.oversampleFactor =
        parseDoubleEnv("PLAYWRIGHT_DIAGRAM_OVERSAMPLE", 1.0d); // apply zoom inside diagram if >1
  }

  /**
   * Creates a DiagramExporter from environment variables.
   *
   * @param workspaceId the workspace ID to export
   * @return configured DiagramExporter or null if environment variables are missing
   */
  public static DiagramExporter fromEnvironment(String workspaceId) {
    String url = System.getenv("STRUCTURIZR_URL");
    String user = System.getenv("STRUCTURIZR_USERNAME");
    String password = System.getenv("STRUCTURIZR_PASSWORD");

    if (url == null || user == null || password == null) {
      logger.warn(
          "STRUCTURIZR_URL, STRUCTURIZR_USERNAME or STRUCTURIZR_PASSWORD not defined. Diagram export unavailable.");
      return null;
    }

    return new DiagramExporter(url, user, password, workspaceId);
  }

  /**
   * Exports all diagrams from the workspace using Playwright.
   *
   * @param workspace the workspace to analyze for diagram export
   * @return list of exported diagram files
   * @throws IOException if export fails
   */
  public List<File> exportDiagrams(Workspace workspace) throws IOException {
    logger.info("Starting diagram export using Playwright for workspace {}", workspaceId);

    // Create output directory
    Files.createDirectories(outputDirectory);

    // Construct workspace URL
    String workspaceUrl = structurizrUrl;
    if (!workspaceUrl.endsWith("/")) {
      workspaceUrl += "/";
    }
    workspaceUrl += "workspace/" + workspaceId;

    logger.info("Exporting diagrams from: {}", workspaceUrl);

    List<File> exportedFiles = new ArrayList<>();

    try (Playwright playwright = Playwright.create()) {
      Browser browser =
          playwright
              .chromium()
              .launch(
                  new BrowserType.LaunchOptions()
                      .setHeadless(true)
                      .setArgs(
                          List.of(
                              "--no-sandbox",
                              "--disable-setuid-sandbox",
                              "--disable-dev-shm-usage")));

      BrowserContext context;

      // Configure to ignore HTTPS errors if SSL verification is disabled
      Browser.NewContextOptions ctxOptions =
          new Browser.NewContextOptions()
              .setViewportSize(viewportWidth, viewportHeight)
              .setDeviceScaleFactor(deviceScaleFactor);
      if (SslTrustUtils.shouldDisableSslVerification()) {
        ctxOptions.setIgnoreHTTPSErrors(true);
        logger.warn("HTTPS certificate errors will be ignored in Playwright browser context");
      }
      context = browser.newContext(ctxOptions);
      logger.info(
          "Playwright context created (viewport={}x{}, deviceScaleFactor={})",
          viewportWidth,
          viewportHeight,
          deviceScaleFactor);

      Page page = context.newPage();

      // Sign in if credentials provided
      if (username != null && password != null) {
        logger.info("Authenticating with Structurizr...");
        signIn(page, workspaceUrl);
      }

      // Navigate to diagram viewer
      String viewerUrl = workspaceUrl;
      if (workspaceUrl.matches(".*/workspace/\\d+/?$")) {
        viewerUrl = workspaceUrl.replaceAll("/?$", "") + "/diagrams";
      }

      logger.info("Opening diagram viewer: {}", viewerUrl);
      page.navigate(viewerUrl);

      // Wait for Structurizr to load
      Frame structurizrFrame = findStructurizrFrame(page);
      if (structurizrFrame == null) {
        throw new IOException(
            "Could not find Structurizr frame after waiting 60 seconds. "
                + "This usually indicates that the Structurizr workspace is not loading properly. "
                + "Check that the workspace URL is correct and accessible: "
                + viewerUrl);
      }

      // Wait for diagrams to be rendered
      structurizrFrame.waitForFunction(
          "() => window.structurizr && window.structurizr.scripting && window.structurizr.scripting.isDiagramRendered && window.structurizr.scripting.isDiagramRendered() === true",
          null,
          new Frame.WaitForFunctionOptions().setTimeout(60000));

      // Get views from Structurizr
      Object viewsResult =
          structurizrFrame.evaluate("() => window.structurizr.scripting.getViews()");
      @SuppressWarnings("unchecked")
      List<Object> views = (List<Object>) viewsResult;

      logger.info("Found {} views to export", views.size());

      // Optional oversampling (apply internal zoom before screenshot for sharper text if needed)
      if (oversampleFactor > 1.0d) {
        try {
          structurizrFrame.evaluate(
              "(z) => { try { if (window.structurizr && window.structurizr.scripting && window.structurizr.scripting.setZoom) { window.structurizr.scripting.setZoom(Math.min(400, z*100)); } } catch(e) {} }",
              oversampleFactor);
          logger.info(
              "Applied oversample zoom factor {} inside Structurizr viewer", oversampleFactor);
        } catch (Exception e) {
          logger.warn(
              "Failed to apply oversample zoom factor {}: {}", oversampleFactor, e.getMessage());
        }
      }

      // Export each view
      int exportCount = 0;
      for (Object viewObj : views) {
        exportCount += exportView(structurizrFrame, viewObj, exportedFiles);
      }

      logger.info("Exported {} diagrams successfully", exportCount);

    } catch (Exception e) {
      throw new IOException("Diagram export failed: " + e.getMessage(), e);
    }

    return exportedFiles;
  }

  /** Sign in to Structurizr. */
  private void signIn(Page page, String workspaceUrl) throws IOException {
    try {
      // Extract base URL for sign in
      String[] parts = workspaceUrl.split("://");
      String host = parts[0] + "://" + parts[1].substring(0, parts[1].indexOf('/'));
      String signinUrl = host + "/signin";

      logger.info("Signing in via: {}", signinUrl);
      page.navigate(signinUrl);

      // Try to find username/email field
      Locator usernameField = null;
      String[] userSelectors = {
        "#username",
        "input[name=\"username\"]",
        "#email",
        "#emailAddress",
        "input[type=\"email\"]",
        "input[name=\"email\"]"
      };
      for (String selector : userSelectors) {
        try {
          usernameField = page.locator(selector);
          if (usernameField.count() > 0) {
            break;
          }
        } catch (Exception ignored) {
        }
      }

      // Try to find password field
      Locator passwordField = null;
      String[] passSelectors = {
        "#password", "input[name=\"password\"]", "input[type=\"password\"]"
      };
      for (String selector : passSelectors) {
        try {
          passwordField = page.locator(selector);
          if (passwordField.count() > 0) {
            break;
          }
        } catch (Exception ignored) {
        }
      }

      if (usernameField == null
          || passwordField == null
          || usernameField.count() == 0
          || passwordField.count() == 0) {
        logger.warn("Could not find login fields, current URL: {}", page.url());
        return;
      }

      // Fill credentials
      usernameField.fill(username);
      passwordField.fill(password);

      // Submit form
      try {
        String[] submitSelectors = {"button[type=\"submit\"]", "input[type=\"submit\"]"};
        boolean clicked = false;
        for (String selector : submitSelectors) {
          try {
            Locator submitBtn = page.locator(selector);
            if (submitBtn.count() > 0) {
              submitBtn.click();
              clicked = true;
              break;
            }
          } catch (Exception ignored) {
          }
        }
        if (!clicked) {
          passwordField.press("Enter");
        }

        // Wait for redirect away from signin
        page.waitForFunction(
            "() => !location.pathname.includes('/signin')",
            null,
            new Page.WaitForFunctionOptions().setTimeout(20000));

      } catch (Exception e) {
        logger.warn("Authentication might have failed: {}", e.getMessage());
      }

    } catch (Exception e) {
      throw new IOException("Sign in failed: " + e.getMessage(), e);
    }
  }

  /** Find the frame containing Structurizr scripting. */
  private Frame findStructurizrFrame(Page page) {
    int maxWaitTime = 60000; // 60 seconds
    int checkInterval = 500; // 0.5 seconds
    long startTime = System.currentTimeMillis();

    logger.debug("Searching for Structurizr frame on page: {}", page.url());

    while (System.currentTimeMillis() - startTime < maxWaitTime) {
      List<Frame> frames = page.frames();
      logger.debug("Found {} frames on page", frames.size());

      for (Frame frame : frames) {
        try {
          Object result =
              frame.evaluate("() => !!(window.structurizr && window.structurizr.scripting)");
          if (Boolean.TRUE.equals(result)) {
            logger.info("Found Structurizr frame");
            return frame;
          } else {
            // Check what's available in the frame for debugging
            try {
              Object structurizrExists = frame.evaluate("() => !!window.structurizr");
              Object scriptingExists =
                  frame.evaluate("() => !!(window.structurizr && window.structurizr.scripting)");
              logger.debug(
                  "Frame {}: structurizr={}, scripting={}",
                  frame.url(),
                  structurizrExists,
                  scriptingExists);
            } catch (Exception debugException) {
              logger.debug(
                  "Frame {} evaluation failed: {}", frame.url(), debugException.getMessage());
            }
          }
        } catch (Exception ignored) {
          // Frame might not be ready yet
          logger.debug("Frame evaluation failed for {}: {}", frame.url(), ignored.getMessage());
        }
      }

      try {
        Thread.sleep(checkInterval);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        break;
      }
    }

    // Provide detailed diagnostics when frame is not found
    logger.warn(
        "Could not find Structurizr frame after {} seconds. Page URL: {}",
        maxWaitTime / 1000,
        page.url());
    List<Frame> finalFrames = page.frames();
    logger.warn("Final state: {} frames found", finalFrames.size());
    for (int i = 0; i < finalFrames.size(); i++) {
      Frame frame = finalFrames.get(i);
      try {
        String title = (String) frame.evaluate("() => document.title || 'No title'");
        logger.warn("Frame {}: URL={}, Title={}", i, frame.url(), title);
      } catch (Exception e) {
        logger.warn("Frame {}: URL={}, Error getting title: {}", i, frame.url(), e.getMessage());
      }
    }

    return null;
  }

  /** Export a single view as PNG. */
  @SuppressWarnings("unchecked")
  private int exportView(Frame structurizrFrame, Object viewObj, List<File> exportedFiles)
      throws IOException {
    try {
      // Convert view object to map for property access
      // This is a simplified approach - in real implementation you'd need proper JSON handling
      String viewKey = extractViewProperty(viewObj, "key");
      String viewType = extractViewProperty(viewObj, "type");

      if (viewKey == null) {
        logger.warn("Skipping view without key");
        return 0;
      }

      logger.info("Exporting view: {} (type: {})", viewKey, viewType);

      // Change to the view
      structurizrFrame.evaluate("(v) => window.structurizr.scripting.changeView(v)", viewKey);

      // Wait for diagram to be rendered
      Thread.sleep(2000); // Give it time to render

      int exportCount = 0;

      // Export diagram
      String diagramFilename = "structurizr-" + workspaceId + "-" + viewKey + ".png";
      Path diagramPath = outputDirectory.resolve(diagramFilename);

      // Take screenshot of only the diagram SVG element for a cleaner export
      // Try multiple selectors in order of preference
      Locator diagramElement = null;
      String[] selectors = {
        "svg#canvas", // Most specific - the actual diagram canvas
        "svg", // Any SVG element (usually the diagram)
        ".structurizrDiagram svg",
        ".diagram svg",
        "#canvasContainer svg",
        ".structurizrDiagram",
        ".diagram"
      };

      for (String selector : selectors) {
        try {
          Locator locator = structurizrFrame.locator(selector).first();
          if (locator.count() > 0) {
            diagramElement = locator;
            logger.debug("Found diagram element using selector: {}", selector);
            break;
          }
        } catch (Exception e) {
          logger.debug("Selector {} did not match: {}", selector, e.getMessage());
        }
      }

      if (diagramElement != null) {
        // High quality screenshot: ensure no background clipping & full element
        Locator.ScreenshotOptions shotOptions =
            new Locator.ScreenshotOptions().setPath(diagramPath);
        diagramElement.screenshot(shotOptions);
        logger.info("Exported diagram for view {} to {}", viewKey, diagramFilename);
      } else {
        logger.warn(
            "Could not find diagram element for view {}, taking full page screenshot", viewKey);
        structurizrFrame.page().screenshot(new Page.ScreenshotOptions().setPath(diagramPath));
      }

      exportedFiles.add(diagramPath.toFile());
      exportCount++;

      // Export key if not an image view
      if (!"Image".equals(viewType)) {
        String keyFilename = "structurizr-" + workspaceId + "-" + viewKey + "-key.png";
        Path keyPath = outputDirectory.resolve(keyFilename);

        // For the key, we'd need to find the key element and screenshot it
        // This is a simplified implementation - you might need to locate the key element
        // specifically
        try {
          Locator keyElement =
              structurizrFrame.locator(".structurizrKey, .key, [class*='key']").first();
          if (keyElement.count() > 0) {
            keyElement.screenshot(new Locator.ScreenshotOptions().setPath(keyPath));
            exportedFiles.add(keyPath.toFile());
            exportCount++;
          }
        } catch (Exception e) {
          logger.debug("Could not export key for view {}: {}", viewKey, e.getMessage());
        }
      }

      logger.debug("Exported {} files for view {}", exportCount, viewKey);
      return exportCount;

    } catch (Exception e) {
      logger.error("Failed to export view: {}", e.getMessage(), e);
      return 0;
    }
  }

  /** Extract a property from a view object (simplified implementation). */
  private String extractViewProperty(Object viewObj, String property) {
    try {
      // In a real implementation, you'd properly parse the JavaScript object
      // This is a simplified version that assumes string representation parsing
      String viewStr = viewObj.toString();
      if (viewStr.contains(property + "=")) {
        // Basic parsing - in production you'd want proper JSON handling
        String[] parts = viewStr.split(property + "=");
        if (parts.length > 1) {
          String value = parts[1].split("[,}]")[0].trim();
          return value.replaceAll("[\"']", "");
        }
      }
      return null;
    } catch (Exception e) {
      logger.debug("Could not extract property {} from view: {}", property, e.getMessage());
      return null;
    }
  }

  /** Parses an integer environment variable with a default value. */
  private static int parseIntEnv(String name, int defaultValue) {
    String value = System.getenv(name);
    if (value == null || value.isBlank()) {
      return defaultValue;
    }
    try {
      return Integer.parseInt(value.trim());
    } catch (NumberFormatException e) {
      LoggerFactory.getLogger(DiagramExporter.class)
          .warn("Invalid integer for env {}: '{}'. Using default {}.", name, value, defaultValue);
      return defaultValue;
    }
  }

  /** Parses a double environment variable with a default value. */
  private static double parseDoubleEnv(String name, double defaultValue) {
    String value = System.getenv(name);
    if (value == null || value.isBlank()) {
      return defaultValue;
    }
    try {
      return Double.parseDouble(value.trim());
    } catch (NumberFormatException e) {
      LoggerFactory.getLogger(DiagramExporter.class)
          .warn("Invalid double for env {}: '{}'. Using default {}.", name, value, defaultValue);
      return defaultValue;
    }
  }

  /**
   * Gets the output directory where diagrams are stored.
   *
   * @return output directory path
   */
  public Path getOutputDirectory() {
    return outputDirectory;
  }

  /**
   * Cleans up exported diagram files.
   *
   * @throws IOException if cleanup fails
   */
  public void cleanup() throws IOException {
    if (Files.exists(outputDirectory)) {
      Files.walk(outputDirectory)
          .sorted((path1, path2) -> path2.compareTo(path1)) // Delete files before directories
          .forEach(
              path -> {
                try {
                  Files.delete(path);
                } catch (IOException e) {
                  logger.warn("Failed to delete: {}", path, e);
                }
              });
    }
  }
}
