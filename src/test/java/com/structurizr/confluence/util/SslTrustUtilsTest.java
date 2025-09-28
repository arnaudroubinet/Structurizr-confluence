package com.structurizr.confluence.util;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import java.net.http.HttpClient;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test for SslTrustUtils functionality
 */
class SslTrustUtilsTest {
    private static final Logger logger = LoggerFactory.getLogger(SslTrustUtilsTest.class);

    @Test
    void testShouldDisableSslVerification() {
        // Initially should be false
        assertFalse(SslTrustUtils.shouldDisableSslVerification());
        
        // Set system property and test
        System.setProperty("disable.ssl.verification", "true");
        assertTrue(SslTrustUtils.shouldDisableSslVerification());
        
        // Clean up
        System.clearProperty("disable.ssl.verification");
        assertFalse(SslTrustUtils.shouldDisableSslVerification());
        
        logger.info("✅ SSL verification check validated");
    }
    
    @Test
    void testCreateTrustAllSslContext() {
        assertDoesNotThrow(() -> {
            SSLContext sslContext = SslTrustUtils.createTrustAllSslContext();
            assertNotNull(sslContext, "SSL context should not be null");
            assertEquals("TLS", sslContext.getProtocol(), "SSL context should use TLS protocol");
        });
        
        logger.info("✅ Trust-all SSL context creation validated");
    }
    
    @Test
    void testCreateTrustAllHttpClient() {
        assertDoesNotThrow(() -> {
            HttpClient httpClient = SslTrustUtils.createTrustAllHttpClient();
            assertNotNull(httpClient, "HTTP client should not be null");
        });
        
        logger.info("✅ Trust-all HTTP client creation validated");
    }
    
    @Test
    void testCreateTrustAllHostnameVerifier() {
        assertDoesNotThrow(() -> {
            var verifier = SslTrustUtils.createTrustAllHostnameVerifier();
            assertNotNull(verifier, "Hostname verifier should not be null");
            // Test that it accepts any hostname
            assertTrue(verifier.verify("localhost", null));
            assertTrue(verifier.verify("example.com", null));
            assertTrue(verifier.verify("invalid-host", null));
        });
        
        logger.info("✅ Trust-all hostname verifier validated");
    }
}