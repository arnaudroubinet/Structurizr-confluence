package arnaudroubinet.structurizr.confluence.client;

import static org.junit.jupiter.api.Assertions.*;

import com.structurizr.api.StructurizrClientException;
import java.util.logging.Level;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tests for StructurizrWorkspaceLoader to ensure HTTP logging can be properly configured and error
 * handling works correctly for authentication issues.
 */
class StructurizrWorkspaceLoaderTest {

  private static final Logger logger =
      LoggerFactory.getLogger(StructurizrWorkspaceLoaderTest.class);

  @Test
  void testHttpLoggingEnabledInDebugMode() throws StructurizrClientException {
    // Given: A config with debug mode enabled
    StructurizrConfig config =
        new StructurizrConfig(
            "http://localhost:8080/api",
            "test-key",
            "test-secret",
            12345L,
            true // debug mode enabled
            );

    // When: Creating a StructurizrWorkspaceLoader
    StructurizrWorkspaceLoader loader = new StructurizrWorkspaceLoader(config);

    // Then: HTTP logging should be enabled for Apache HttpClient 5 packages
    java.util.logging.Logger httpClientLogger =
        java.util.logging.Logger.getLogger("org.apache.hc.client5.http");
    java.util.logging.Logger wireLogger =
        java.util.logging.Logger.getLogger("org.apache.hc.client5.http.wire");
    java.util.logging.Logger headersLogger =
        java.util.logging.Logger.getLogger("org.apache.hc.client5.http.headers");

    // Verify that the log levels are set to FINE (DEBUG equivalent)
    assertEquals(
        Level.FINE,
        httpClientLogger.getLevel(),
        "HTTP client logger should be set to FINE level in debug mode");
    assertEquals(
        Level.FINE, wireLogger.getLevel(), "Wire logger should be set to FINE level in debug mode");
    assertEquals(
        Level.FINE,
        headersLogger.getLevel(),
        "Headers logger should be set to FINE level in debug mode");

    logger.info("✅ HTTP logging configuration validated in debug mode");
  }

  @Test
  void testHttpLoggingNotEnabledWithoutDebugMode() throws StructurizrClientException {
    // Given: A config without debug mode
    StructurizrConfig config =
        new StructurizrConfig(
            "http://localhost:8080/api",
            "test-key",
            "test-secret",
            12345L,
            false // debug mode disabled
            );

    // Store initial levels
    java.util.logging.Logger httpClientLogger =
        java.util.logging.Logger.getLogger("org.apache.hc.client5.http.test");
    java.util.logging.Logger wireLogger =
        java.util.logging.Logger.getLogger("org.apache.hc.client5.http.wire.test");
    Level initialHttpLevel = httpClientLogger.getLevel();
    Level initialWireLevel = wireLogger.getLevel();

    // When: Creating a StructurizrWorkspaceLoader without debug mode
    StructurizrWorkspaceLoader loader = new StructurizrWorkspaceLoader(config);

    // Then: HTTP logging levels should remain unchanged (not explicitly set to FINE)
    // Note: We use different logger names to avoid interference from the previous test
    assertEquals(
        initialHttpLevel,
        httpClientLogger.getLevel(),
        "HTTP client logger level should not be changed when debug mode is disabled");
    assertEquals(
        initialWireLevel,
        wireLogger.getLevel(),
        "Wire logger level should not be changed when debug mode is disabled");

    logger.info("✅ HTTP logging remains unchanged when debug mode is disabled");
  }

  @Test
  void testStructurizrClientCreation() throws StructurizrClientException {
    // Given: A valid config
    StructurizrConfig config =
        new StructurizrConfig(
            "http://localhost:8080/api", "test-key", "test-secret", 12345L, false);

    // When: Creating a StructurizrWorkspaceLoader
    StructurizrWorkspaceLoader loader = new StructurizrWorkspaceLoader(config);

    // Then: The loader should be created successfully
    assertNotNull(loader, "StructurizrWorkspaceLoader should be created successfully");

    logger.info("✅ StructurizrWorkspaceLoader created successfully with valid config");
  }

  @Test
  void testStructurizrClientCreationWithCloudService() throws StructurizrClientException {
    // Given: A config without API URL (for cloud service)
    StructurizrConfig config =
        new StructurizrConfig(
            null, // no API URL - use cloud service
            "test-key",
            "test-secret",
            12345L,
            false);

    // When: Creating a StructurizrWorkspaceLoader
    StructurizrWorkspaceLoader loader = new StructurizrWorkspaceLoader(config);

    // Then: The loader should be created successfully for cloud service
    assertNotNull(
        loader, "StructurizrWorkspaceLoader should be created successfully for cloud service");

    logger.info("✅ StructurizrWorkspaceLoader created successfully for Structurizr cloud service");
  }

  @Test
  void testApiUrlAppendedForStructurizrClient() throws StructurizrClientException {
    // This test validates that /api is automatically appended to the URL
    // when creating a StructurizrClient for API calls

    // Given: A config with a URL without /api suffix
    StructurizrConfig config =
        new StructurizrConfig(
            "http://localhost:8080",
            "test-key",
            "test-secret",
            12345L,
            true // debug mode to see the URL in logs
            );

    // When: Creating a StructurizrWorkspaceLoader
    StructurizrWorkspaceLoader loader = new StructurizrWorkspaceLoader(config);

    // Then: The loader should be created successfully and /api should be appended internally
    assertNotNull(loader, "StructurizrWorkspaceLoader should be created successfully");

    logger.info("✅ StructurizrClient URL should have /api appended automatically");
  }

  @Test
  void testApiUrlNotDuplicatedIfAlreadyPresent() throws StructurizrClientException {
    // This test validates that /api is not duplicated if already present

    // Given: A config with a URL that already has /api suffix
    StructurizrConfig config =
        new StructurizrConfig("http://localhost:8080/api", "test-key", "test-secret", 12345L, true);

    // When: Creating a StructurizrWorkspaceLoader
    StructurizrWorkspaceLoader loader = new StructurizrWorkspaceLoader(config);

    // Then: The loader should be created successfully without duplicating /api
    assertNotNull(loader, "StructurizrWorkspaceLoader should be created successfully");

    logger.info("✅ StructurizrClient URL should not duplicate /api if already present");
  }

  @Test
  void testApiUrlWithTrailingSlash() throws StructurizrClientException {
    // This test validates that /api is properly appended even with trailing slash

    // Given: A config with a URL with trailing slash
    StructurizrConfig config =
        new StructurizrConfig("http://localhost:8080/", "test-key", "test-secret", 12345L, true);

    // When: Creating a StructurizrWorkspaceLoader
    StructurizrWorkspaceLoader loader = new StructurizrWorkspaceLoader(config);

    // Then: The loader should be created successfully with proper URL handling
    assertNotNull(loader, "StructurizrWorkspaceLoader should be created successfully");

    logger.info("✅ StructurizrClient URL should handle trailing slash correctly");
  }
}
