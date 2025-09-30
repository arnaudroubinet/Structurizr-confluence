package arnaudroubinet.structurizr.confluence.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.*;
import java.net.http.HttpClient;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;

/**
 * Utility class for handling SSL trust configuration, particularly for self-signed certificates.
 */
public class SslTrustUtils {
    
    private static final Logger logger = LoggerFactory.getLogger(SslTrustUtils.class);
    
    /**
     * Creates an HttpClient that trusts all certificates (including self-signed ones).
     * WARNING: This should only be used in development or when connecting to trusted endpoints with self-signed certificates.
     * 
     * @return HttpClient configured to trust all certificates
     * @throws RuntimeException if SSL configuration fails
     */
    public static HttpClient createTrustAllHttpClient() {
        try {
            TrustManager[] trustAllCerts = createTrustAllTrustManagers();
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            
            return HttpClient.newBuilder()
                .sslContext(sslContext)
                .build();
                
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            throw new RuntimeException("Failed to create trust-all SSL context", e);
        }
    }
    
    /**
     * Creates an SSLContext that trusts all certificates (including self-signed ones).
     * WARNING: This should only be used in development or when connecting to trusted endpoints with self-signed certificates.
     * 
     * @return SSLContext configured to trust all certificates
     * @throws NoSuchAlgorithmException if TLS algorithm is not available
     * @throws KeyManagementException if SSL context initialization fails
     */
    public static SSLContext createTrustAllSslContext() throws NoSuchAlgorithmException, KeyManagementException {
        TrustManager[] trustAllCerts = createTrustAllTrustManagers();
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
        return sslContext;
    }
    
    /**
     * Creates TrustManager array that trusts all certificates.
     * 
     * @return TrustManager array that accepts all certificates
     */
    private static TrustManager[] createTrustAllTrustManagers() {
        return new TrustManager[] {
            new X509TrustManager() {
                @Override
                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }
                
                @Override
                public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {
                    // Trust all client certificates
                }
                
                @Override
                public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {
                    // Trust all server certificates
                    logger.debug("Accepting server certificate: {}", 
                        certs.length > 0 ? certs[0].getSubjectDN().getName() : "unknown");
                }
            }
        };
    }
    
    /**
     * Creates a HostnameVerifier that accepts all hostnames.
     * WARNING: This should only be used in development or when connecting to trusted endpoints.
     * 
     * @return HostnameVerifier that accepts all hostnames
     */
    public static HostnameVerifier createTrustAllHostnameVerifier() {
        return (hostname, session) -> {
            logger.debug("Accepting hostname: {}", hostname);
            return true;
        };
    }
    
    /**
     * Checks if SSL certificate validation should be disabled based on environment variables or system properties.
     * Checks for DISABLE_SSL_VERIFICATION environment variable or javax.net.ssl.trustStore system property.
     * 
     * @return true if SSL verification should be disabled
     */
    public static boolean shouldDisableSslVerification() {
        // Check environment variable first
        String disableEnv = System.getenv("DISABLE_SSL_VERIFICATION");
        if ("true".equalsIgnoreCase(disableEnv)) {
            logger.warn("SSL certificate verification disabled via DISABLE_SSL_VERIFICATION environment variable");
            return true;
        }
        
        // Check system property
        String disableProp = System.getProperty("disable.ssl.verification");
        if ("true".equalsIgnoreCase(disableProp)) {
            logger.warn("SSL certificate verification disabled via disable.ssl.verification system property");
            return true;
        }
        
        return false;
    }
    
    /**
     * Installs the trust-all SSL context as the default SSL context for the JVM.
     * WARNING: This affects all SSL connections in the application.
     * This should only be used when necessary for self-signed certificate environments.
     */
    public static void installTrustAllSslContext() {
        try {
            SSLContext trustAllSslContext = createTrustAllSslContext();
            SSLContext.setDefault(trustAllSslContext);
            
            // Also set default hostname verifier
            HttpsURLConnection.setDefaultHostnameVerifier(createTrustAllHostnameVerifier());
            
            logger.warn("Global SSL verification disabled - all certificates will be trusted");
        } catch (Exception e) {
            logger.error("Failed to install trust-all SSL context", e);
            throw new RuntimeException("Failed to configure SSL trust settings", e);
        }
    }
}