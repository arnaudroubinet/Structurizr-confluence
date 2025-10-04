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

  // Environment variable names
  private static final String ENV_STRUCTURIZR_URL = "STRUCTURIZR_URL";
  private static final String ENV_STRUCTURIZR_USERNAME = "STRUCTURIZR_USERNAME";
  private static final String ENV_STRUCTURIZR_PASSWORD = "STRUCTURIZR_PASSWORD";

  // Configuration defaults
  private static final int DEFAULT_MAX_DURATION_SECONDS = 300;
  private static final int DEFAULT_VIEWPORT_WIDTH = 1920;
  private static final int DEFAULT_VIEWPORT_HEIGHT = 1080;
  private static final double DEFAULT_DEVICE_SCALE_FACTOR = 2.0;
  private static final double DEFAULT_OVERSAMPLE_FACTOR = 1.0;
  private static final int FRAME_WAIT_TIMEOUT_MS = 60000;
  private static final int AUTH_WAIT_TIMEOUT_MS = 20000;
  private static final int FRAME_CHECK_INTERVAL_MS = 500;
  private static final int DIAGRAM_RENDER_WAIT_MS = 2000;

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
    this.maxDurationSeconds =
        parseIntEnv("PLAYWRIGHT_MAX_DURATION_SECS", DEFAULT_MAX_DURATION_SECONDS);
    this.viewportWidth = parseIntEnv("PLAYWRIGHT_VIEWPORT_WIDTH", DEFAULT_VIEWPORT_WIDTH);
    this.viewportHeight = parseIntEnv("PLAYWRIGHT_VIEWPORT_HEIGHT", DEFAULT_VIEWPORT_HEIGHT);
    this.deviceScaleFactor = parseDoubleEnv("PLAYWRIGHT_DEVICE_SCALE", DEFAULT_DEVICE_SCALE_FACTOR);
    this.oversampleFactor =
        parseDoubleEnv("PLAYWRIGHT_DIAGRAM_OVERSAMPLE", DEFAULT_OVERSAMPLE_FACTOR);
  }

  /**
   * Creates a DiagramExporter from environment variables.
   *
   * @param workspaceId the workspace ID to export
   * @return configured DiagramExporter or null if environment variables are missing
   */
  public static DiagramExporter fromEnvironment(String workspaceId) {
    String url = System.getenv(ENV_STRUCTURIZR_URL);
    String user = System.getenv(ENV_STRUCTURIZR_USERNAME);
    String password = System.getenv(ENV_STRUCTURIZR_PASSWORD);

    if (url == null || user == null || password == null) {
      logger.warn(
          "{}, {} or {} not defined. Diagram export unavailable.",
          ENV_STRUCTURIZR_URL,
          ENV_STRUCTURIZR_USERNAME,
          ENV_STRUCTURIZR_PASSWORD);
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
            "Could not find Structurizr frame after waiting "
                + (FRAME_WAIT_TIMEOUT_MS / 1000)
                + " seconds. "
                + "This usually indicates that the Structurizr workspace is not loading properly. "
                + "Check that the workspace URL is correct and accessible: "
                + viewerUrl);
      }

      structurizrFrame.waitForFunction(
          "() => window.structurizr && window.structurizr.scripting && window.structurizr.scripting.isDiagramRendered && window.structurizr.scripting.isDiagramRendered() === true",
          null,
          new Frame.WaitForFunctionOptions().setTimeout(FRAME_WAIT_TIMEOUT_MS));

      // Get views from Structurizr
      Object viewsResult =
          structurizrFrame.evaluate("() => window.structurizr.scripting.getViews()");
      @SuppressWarnings("unchecked")
      List<Object> views = (List<Object>) viewsResult;

      logger.info("Found {} views to export", views.size());

      // Optional oversampling (apply internal zoom before screenshot for sharper text if needed)
      if (oversampleFactor > 1.0) {
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

  private void signIn(Page page, String workspaceUrl) throws IOException {
    try {
      String[] parts = workspaceUrl.split("://");
      String host = parts[0] + "://" + parts[1].substring(0, parts[1].indexOf('/'));
      String signinUrl = host + "/signin";

      logger.info("Signing in via: {}", signinUrl);
      page.navigate(signinUrl);

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

      usernameField.fill(username);
      passwordField.fill(password);

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

        page.waitForFunction(
            "() => !location.pathname.includes('/signin')",
            null,
            new Page.WaitForFunctionOptions().setTimeout(AUTH_WAIT_TIMEOUT_MS));

      } catch (Exception e) {
        logger.warn("Authentication might have failed: {}", e.getMessage());
      }

    } catch (Exception e) {
      throw new IOException("Sign in failed: " + e.getMessage(), e);
    }
  }

  private Frame findStructurizrFrame(Page page) {
    long startTime = System.currentTimeMillis();

    logger.debug("Searching for Structurizr frame on page: {}", page.url());

    while (System.currentTimeMillis() - startTime < FRAME_WAIT_TIMEOUT_MS) {
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
          logger.debug("Frame evaluation failed for {}: {}", frame.url(), ignored.getMessage());
        }
      }

      try {
        Thread.sleep(FRAME_CHECK_INTERVAL_MS);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        break;
      }
    }

    logger.warn(
        "Could not find Structurizr frame after {} seconds. Page URL: {}",
        FRAME_WAIT_TIMEOUT_MS / 1000,
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

      structurizrFrame.evaluate("(v) => window.structurizr.scripting.changeView(v)", viewKey);

      Thread.sleep(DIAGRAM_RENDER_WAIT_MS);

      int exportCount = 0;

      String diagramFilename = "structurizr-" + workspaceId + "-" + viewKey + ".png";
      Path diagramPath = outputDirectory.resolve(diagramFilename);

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

      if (!"Image".equals(viewType)) {
        String keyFilename = "structurizr-" + workspaceId + "-" + viewKey + "-key.png";
        Path keyPath = outputDirectory.resolve(keyFilename);

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

  private String extractViewProperty(Object viewObj, String property) {
    try {
      String viewStr = viewObj.toString();
      if (viewStr.contains(property + "=")) {
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

  public void cleanup() throws IOException {
    if (Files.exists(outputDirectory)) {
      Files.walk(outputDirectory)
          .sorted((path1, path2) -> path2.compareTo(path1))
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
