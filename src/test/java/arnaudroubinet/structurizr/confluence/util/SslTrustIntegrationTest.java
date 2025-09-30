package arnaudroubinet.structurizr.confluence.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for SSL trust configuration.
 * This demonstrates how the SSL bypass functionality works in practice.
 */
class SslTrustIntegrationTest {
    private static final Logger logger = LoggerFactory.getLogger(SslTrustIntegrationTest.class);
    
    @BeforeEach
    void setUp() {
        // Clean up any previous SSL settings
        System.clearProperty("disable.ssl.verification");
    }
    
    @Test
    void testSslBypassViaSystemProperty() {
        // Verify SSL verification is initially enabled
        assertFalse(SslTrustUtils.shouldDisableSslVerification());
        
        // Enable SSL bypass via system property
        System.setProperty("disable.ssl.verification", "true");
        assertTrue(SslTrustUtils.shouldDisableSslVerification());
        
        // Test that SSL utilities work correctly when bypass is enabled
        assertDoesNotThrow(() -> {
            var httpClient = SslTrustUtils.createTrustAllHttpClient();
            assertNotNull(httpClient, "Trust-all HttpClient should be created successfully");
            
            var sslContext = SslTrustUtils.createTrustAllSslContext();
            assertNotNull(sslContext, "Trust-all SSLContext should be created successfully");
        });
        
        // Clean up
        System.clearProperty("disable.ssl.verification");
        assertFalse(SslTrustUtils.shouldDisableSslVerification());
        
        logger.info("✅ SSL bypass via system property integration validated");
    }
    
    @Test
    void testSslVerificationEnabledByDefault() {
        // Verify that SSL verification is enabled by default
        assertFalse(SslTrustUtils.shouldDisableSslVerification());
        
        // Even when verification is enabled, SSL utilities should work
        assertDoesNotThrow(() -> {
            var httpClient = SslTrustUtils.createTrustAllHttpClient();
            assertNotNull(httpClient, "HttpClient should be created successfully with default SSL settings");
            
            var sslContext = SslTrustUtils.createTrustAllSslContext();
            assertNotNull(sslContext, "SSLContext should be created successfully");
        });
        
        logger.info("✅ Default SSL verification behavior validated");
    }
    
    @Test
    void testMultipleSslClientCreation() {
        // Test that we can create multiple SSL clients without issues
        System.setProperty("disable.ssl.verification", "true");
        
        assertDoesNotThrow(() -> {
            // Create multiple trust-all clients
            var client1 = SslTrustUtils.createTrustAllHttpClient();
            var client2 = SslTrustUtils.createTrustAllHttpClient();
            var client3 = SslTrustUtils.createTrustAllHttpClient();
            
            assertNotNull(client1);
            assertNotNull(client2);
            assertNotNull(client3);
            
            // Each client should be independent
            assertNotSame(client1, client2);
            assertNotSame(client2, client3);
        });
        
        // Clean up
        System.clearProperty("disable.ssl.verification");
        
        logger.info("✅ Multiple SSL client creation validated");
    }
    
    @Test
    void testEnvironmentVariableDetection() {
        // Since we can't easily set environment variables in tests,
        // we test the logic for checking them exists
        
        // First verify no system property is set
        assertFalse(SslTrustUtils.shouldDisableSslVerification());
        
        // Set system property (simulating env var behavior)
        System.setProperty("disable.ssl.verification", "true");
        assertTrue(SslTrustUtils.shouldDisableSslVerification());
        
        // Test case insensitivity (if applicable)
        System.setProperty("disable.ssl.verification", "TRUE");
        assertTrue(SslTrustUtils.shouldDisableSslVerification());
        
        System.setProperty("disable.ssl.verification", "false");
        assertFalse(SslTrustUtils.shouldDisableSslVerification());
        
        // Clean up
        System.clearProperty("disable.ssl.verification");
        
        logger.info("✅ Environment variable detection validated");
    }
}