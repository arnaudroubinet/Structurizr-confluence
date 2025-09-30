package arnaudroubinet.structurizr.confluence.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import arnaudroubinet.structurizr.confluence.util.SslTrustUtils;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.inject.Vetoed;
import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Client for interacting with Confluence Cloud REST API.
 */
@Vetoed
public class ConfluenceClient {
    
    private static final Logger logger = LoggerFactory.getLogger(ConfluenceClient.class);
    
    private final ConfluenceConfig config;
    private final ObjectMapper objectMapper;
    private final ConfluenceApi api;

    public ConfluenceClient(ConfluenceConfig config) {
        this.config = config;
        this.objectMapper = new ObjectMapper();
        RestClientBuilder builder = createRestClientBuilder();
        
        // Configure SSL trust settings using Quarkus TLS configuration approach
        if (SslTrustUtils.shouldDisableSslVerification()) {
            try {
                // In Quarkus 3.x, we should use TLS configuration instead of directly setting SSLContext
                // since RestClientBuilder.sslContext() is not supported in newer versions.
                // 
                // We use the global SSL context installation as the most compatible approach
                // across different Quarkus REST client implementations (RESTEasy, SmallRye, etc.)
                logger.warn("SSL certificate verification disabled for Confluence REST client - using global SSL context");
                SslTrustUtils.installTrustAllSslContext();
            } catch (Exception e) {
                logger.error("Failed to configure SSL trust settings for REST client", e);
                throw new RuntimeException("SSL configuration failed", e);
            }
        }
        
        this.api = builder
            .baseUri(normalizeBaseUri(config.getBaseUrl()))
            .register(new AuthHeadersFilter(config.getUsername(), config.getApiToken()))
            .build(ConfluenceApi.class);
    }

    private static RestClientBuilder createRestClientBuilder() {
        // Try RESTEasy MicroProfile implementation first
        try {
            Class<?> impl = Class.forName("org.jboss.resteasy.microprofile.client.RestClientBuilderImpl");
            return (RestClientBuilder) impl.getDeclaredConstructor().newInstance();
        } catch (Throwable ignore) {
            // ignore and try SmallRye next
        }
        try {
            Class<?> impl = Class.forName("io.smallrye.restclient.RestClientBuilderImpl");
            return (RestClientBuilder) impl.getDeclaredConstructor().newInstance();
        } catch (Throwable ignore) {
            // ignore and fallback to SPI (may fail if resolver not present)
        }
        return RestClientBuilder.newBuilder();
    }

    private static URI normalizeBaseUri(String baseUrl) {
        String s = baseUrl == null ? "" : baseUrl.trim();
        if (s.endsWith("/")) s = s.substring(0, s.length() - 1);
        if (s.endsWith("/wiki")) s = s.substring(0, s.length() - 5);
        return URI.create(s);
    }
    
    /**
     * Creates or updates a page in Confluence with ADF content.
     */
    public String createOrUpdatePage(String title, String adfContent) throws IOException {
        return createOrUpdatePage(title, adfContent, null);
    }
    
    /**
     * Creates or updates a page in Confluence with ADF content under a specific parent.
     */
    public String createOrUpdatePage(String title, String adfContent, String parentId) throws IOException {
        // First, check if page exists
        String existingPageId = findPageByTitle(title);
        
        if (existingPageId != null) {
            return updatePage(existingPageId, title, adfContent);
        } else {
            return createPage(title, adfContent, parentId);
        }
    }
    
    private String findPageByTitle(String title) throws IOException {
        try {
            Uni<String> uni = api.findContentByTitle(title, config.getSpaceKey());
            String responseBody = uni.await().indefinitely();
            JsonNode responseJson = objectMapper.readTree(responseBody);
            if (responseJson.has("results") && responseJson.get("results").size() > 0) {
                return responseJson.get("results").get(0).get("id").asText();
            }
        } catch (Exception e) {
            // Check if this is a 404 error (page not found) which is expected when page doesn't exist
            String errorMessage = e.getMessage();
            if (errorMessage != null && (errorMessage.contains("404") || errorMessage.contains("Not Found"))) {
                logger.debug("Page '{}' not found in space '{}' (404 response - this may be expected)", title, config.getSpaceKey());
                return null; // Return null for 404 instead of throwing exception
            }
            // For other errors, provide more context
            throw new IOException("Failed to query Confluence content by title '" + title + 
                "' in space '" + config.getSpaceKey() + "': " + errorMessage, e);
        }
        return null;
    }
    
    private String createPage(String title, String adfContent, String parentId) throws IOException {
        try {
            String spaceId = getSpaceId();
            Map<String, Object> pageData = new HashMap<>();
            pageData.put("spaceId", spaceId);
            pageData.put("status", "current");
            pageData.put("title", title);
            
            if (parentId != null) {
                pageData.put("parentId", parentId);
            }
            
            Map<String, Object> body = new HashMap<>();
            body.put("representation", "atlas_doc_format");
            body.put("value", adfContent);
            pageData.put("body", body);
            
            String jsonBody = objectMapper.writeValueAsString(pageData);
            String responseBody = api.createPage(jsonBody).await().indefinitely();
            JsonNode responseJson = objectMapper.readTree(responseBody);
            String pageId = responseJson.get("id").asText();
            logger.info("Page created successfully with ID: {}", pageId);
            return pageId;
        } catch (Exception e) {
            throw new IOException("Failed to create page", e);
        }
    }
    
    private String updatePage(String pageId, String title, String adfContent) throws IOException {
        try {
            // Get current page version using API v2
            String getResponseBody = api.getPageInfo(pageId).await().indefinitely();
            int currentVersion;
            String spaceId;
            String parentId = null;
            JsonNode getResponseJson = objectMapper.readTree(getResponseBody);
            
            // Check if the version field exists
            JsonNode versionNode = getResponseJson.get("version");
            if (versionNode == null) {
                throw new RuntimeException("Page response missing version field: " + getResponseBody);
            }
            
            JsonNode numberNode = versionNode.get("number");
            if (numberNode == null) {
                throw new RuntimeException("Page version missing number field: " + getResponseBody);
            }
            
            currentVersion = numberNode.asInt();
            spaceId = getResponseJson.get("spaceId").asText();
            
            // Get parentId if exists
            JsonNode parentIdNode = getResponseJson.get("parentId");
            if (parentIdNode != null && !parentIdNode.isNull()) {
                parentId = parentIdNode.asText();
            }
            
            // Update page using API v2
            
            Map<String, Object> pageData = new HashMap<>();
            pageData.put("id", pageId);
            pageData.put("status", "current");
            pageData.put("title", title);
            pageData.put("spaceId", spaceId);
            
            if (parentId != null) {
                pageData.put("parentId", parentId);
            }
            
            Map<String, Object> version = new HashMap<>();
            version.put("number", currentVersion + 1);
            pageData.put("version", version);
            
            Map<String, Object> body = new HashMap<>();
            body.put("representation", "atlas_doc_format");
            body.put("value", adfContent);
            pageData.put("body", body);
            
            String jsonBody = objectMapper.writeValueAsString(pageData);
            api.updatePage(pageId, jsonBody).await().indefinitely();
            logger.info("Page updated successfully with ID: {}", pageId);
            return pageId;
        } catch (Exception e) {
            throw new IOException("Failed to update page", e);
        }
    }

    /**
     * Updates a specific page by ID with ADF content.
     * 
     * @param pageId the ID of the page to update
     * @param title the new title for the page
     * @param adfContent the ADF content to set
     * @return the page ID
     * @throws IOException if the update fails
     */
    public String updatePageById(String pageId, String title, String adfContent) throws IOException {
        return updatePage(pageId, title, adfContent);
    }
    
    /**
     * Gets the space ID from the space key using API v2.
     * 
     * @return the space ID
     * @throws IOException if the request fails
     */
    private String getSpaceId() throws IOException {
        try {
            // Utiliser l'endpoint API v2 pour lister les espaces et filtrer par clé
            logger.info("Getting space ID for space key: {}", config.getSpaceKey());
            String responseBody = api.listSpacesByKeys(config.getSpaceKey()).await().indefinitely();
            JsonNode jsonResponse = objectMapper.readTree(responseBody);
            JsonNode results = jsonResponse.get("results");
            
            if (results == null || results.isEmpty()) {
                throw new IOException("Space not found with key: " + config.getSpaceKey());
            }
            
            String spaceId = results.get(0).get("id").asText();
            logger.info("Found space ID: {} for key: {}", spaceId, config.getSpaceKey());
            return spaceId;
        } catch (Exception e) {
            String msg = "Error getting space ID for key '" + config.getSpaceKey() + "' on base URL '" + config.getBaseUrl() + "'";
            throw new IOException(msg, e);
        }
    }

    /**
     * Lists all pages in the specified space using API v2.
     * 
     * @return an array of page IDs in the space
     * @throws IOException if the request fails
     */
    public String[] listPagesInSpace() throws IOException {
        try {
            String spaceId = getSpaceId();
            
            // Utiliser l'endpoint API v2 avec l'ID de l'espace
            logger.info("Listing pages in space: {} (ID: {})", config.getSpaceKey(), spaceId);
            String responseBody = api.listPages(spaceId, 250).await().indefinitely();
            logger.info("Response from listing pages: {}", responseBody.substring(0, Math.min(500, responseBody.length())));
            JsonNode jsonResponse = objectMapper.readTree(responseBody);
                JsonNode results = jsonResponse.get("results");
                
                if (results == null || results.isEmpty()) {
                    logger.info("No pages found in space: {}", config.getSpaceKey());
                    return new String[0];
                }
                
                String[] pageIds = new String[results.size()];
                for (int i = 0; i < results.size(); i++) {
                    pageIds[i] = results.get(i).get("id").asText();
                }
                
                logger.info("Found {} pages in space {}", pageIds.length, config.getSpaceKey());
                return pageIds;
            
        } catch (Exception e) {
            throw new IOException("Error listing pages in space", e);
        }
    }

    /**
     * Deletes a page by ID using API v2.
     * 
     * @param pageId the ID of the page to delete
     * @throws IOException if the deletion fails
     */
    public void deletePage(String pageId) throws IOException {
        try {
            logger.info("Deleting page with ID: {}", pageId);
            api.deletePage(pageId).await().indefinitely();
            logger.info("Successfully deleted page: {}", pageId);
        } catch (Exception e) {
            logger.error("Error deleting page: {}", pageId, e);
            throw new IOException("Error deleting page: " + pageId, e);
        }
    }
    
    /**
     * Cleans the entire space by deleting all pages.
     * 
     * @throws IOException if the cleanup fails
     */
    public void cleanSpace() throws IOException {
        logger.info("Starting cleanup of Confluence space: {}", config.getSpaceKey());
        
        String[] pageIds = listPagesInSpace();
        
        if (pageIds.length == 0) {
            logger.info("No pages found in space {}, nothing to clean", config.getSpaceKey());
            return;
        }
        
        logger.info("Deleting {} pages from space {}", pageIds.length, config.getSpaceKey());
        
        for (String pageId : pageIds) {
            try {
                deletePage(pageId);
            } catch (IOException e) {
                logger.warn("Failed to delete page {}, continuing with others", pageId, e);
            }
        }
        
        logger.info("Cleanup completed for space: {}", config.getSpaceKey());
    }
    
    /**
     * Gets the list of page IDs in the space.
     * 
     * @param spaceKey the space key
     * @return list of page IDs
     * @throws IOException if the request fails
     */
    public List<String> getSpacePageIds(String spaceKey) throws IOException {
        try {
            String spaceId = getSpaceId();
            String responseBody = api.listPages(spaceId, 250).await().indefinitely();
            JsonNode jsonResponse = objectMapper.readTree(responseBody);
                JsonNode results = jsonResponse.get("results");
                
                List<String> pageIds = new ArrayList<>();
                if (results != null && results.isArray()) {
                    for (JsonNode page : results) {
                        pageIds.add(page.get("id").asText());
                    }
                }
                
                logger.info("Found {} pages in space {}", pageIds.size(), spaceKey);
                return pageIds;
        } catch (Exception e) {
            throw new IOException("Error getting space page IDs", e);
        }
    }
    
    /**
     * Gets page content in ADF format.
     * 
     * @param pageId the page ID
     * @return the page content as ADF JSON string
     * @throws IOException if the request fails
     */
    public String getPageContent(String pageId) throws IOException {
        try {
            String responseBody = api.getPage(pageId, "atlas_doc_format").await().indefinitely();
            JsonNode jsonResponse = objectMapper.readTree(responseBody);
                JsonNode body = jsonResponse.get("body");
                if (body != null && body.get("atlas_doc_format") != null) {
                    // Return the raw ADF JSON string. Using toString() here would include extra quotes/escapes
                    // (e.g., "{\"type\":\"doc\"...}") which breaks string contains checks in tests.
                    return body.get("atlas_doc_format").get("value").asText();
                }
                
                logger.warn("No ADF content found for page {}", pageId);
                return "{}";
        } catch (Exception e) {
            throw new IOException("Error getting page content", e);
        }
    }
    
    /**
     * Gets page information including title.
     * 
     * @param pageId the page ID
     * @return the page info as JSON string
     * @throws IOException if the request fails
     */
    public String getPageInfo(String pageId) throws IOException {
        try {
            String responseBody = api.getPageInfo(pageId).await().indefinitely();
            return responseBody;
        } catch (Exception e) {
            throw new IOException("Error getting page info", e);
        }
    }
    
    /**
     * Checks if a page exists by ID.
     * 
     * @param pageId the page ID to check
     * @return true if the page exists, false otherwise
     */
    public boolean pageExists(String pageId) {
        try {
            api.getPageInfo(pageId).await().indefinitely();
            return true;
        } catch (Exception e) {
            logger.debug("Page with ID {} does not exist: {}", pageId, e.getMessage());
            return false;
        }
    }
    
    /**
     * Uploads an attachment to a Confluence page.
     * 
     * @param pageId the ID of the page to attach the file to
     * @param fileName the name of the file
     * @param fileContent the binary content of the file
     * @param mimeType the MIME type of the file
     * @return the attachment ID
     * @throws IOException if the upload fails
     */
    public String uploadAttachment(String pageId, String fileName, byte[] fileContent, String mimeType) throws IOException {
        try {
            // The Confluence v2 attachment upload requires multipart; with Rest Client Reactive it's easier to call the classic endpoint with query filename
            String boundary = "--------------------------" + System.currentTimeMillis();
            String url = config.getBaseUrl() + "/wiki/rest/api/content/" + pageId + "/child/attachment";
            String encoded = Base64.getEncoder().encodeToString((config.getUsername()+":"+config.getApiToken()).getBytes(StandardCharsets.UTF_8));
            var bodyBuilder = new StringBuilder();
            bodyBuilder.append("--").append(boundary).append("\r\n");
            bodyBuilder.append("Content-Disposition: form-data; name=\"file\"; filename=\"").append(fileName).append("\"\r\n");
            bodyBuilder.append("Content-Type: ").append(mimeType).append("\r\n\r\n");
            byte[] prefix = bodyBuilder.toString().getBytes(StandardCharsets.UTF_8);
            byte[] suffix = ("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8);
            byte[] multipart = new byte[prefix.length + fileContent.length + suffix.length];
            System.arraycopy(prefix, 0, multipart, 0, prefix.length);
            System.arraycopy(fileContent, 0, multipart, prefix.length, fileContent.length);
            System.arraycopy(suffix, 0, multipart, prefix.length + fileContent.length, suffix.length);

            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .header("Authorization", "Basic " + encoded)
                .header("X-Atlassian-Token", "nocheck")
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(multipart))
                .build();

            var client = SslTrustUtils.shouldDisableSslVerification() 
                ? SslTrustUtils.createTrustAllHttpClient() 
                : HttpClient.newHttpClient();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            String responseBody = response.body();
            if (response.statusCode() == 200) {
                JsonNode jsonResponse = objectMapper.readTree(responseBody);
                JsonNode results = jsonResponse.get("results");
                if (results != null && results.isArray() && results.size() > 0) {
                    String attachmentId = results.get(0).get("id").asText();
                    logger.info("Attachment uploaded successfully with ID: {}", attachmentId);
                    return attachmentId;
                }
                throw new IOException("Upload successful but no attachment ID returned");
            }
            throw new IOException("Failed to upload attachment: HTTP " + response.statusCode() + " - " + responseBody);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted", ie);
        } catch (Exception e) {
            throw new IOException("Failed to upload attachment", e);
        }
    }

    /**
     * Detailed upload returning Confluence Media identifiers needed by ADF media nodes.
     * After uploading the attachment, fetches the media fileId and collectionName via expand=extensions.
     */
    public AttachmentDetails uploadAttachmentDetailed(String pageId, String fileName, byte[] fileContent, String mimeType) throws IOException {
        String attachmentId = uploadAttachment(pageId, fileName, fileContent, mimeType);

        // Fetch attachment details with extensions to get media identifiers
        try {
            String responseBody = api.getAttachmentWithExtensions(attachmentId, "extensions").await().indefinitely();
            JsonNode json = objectMapper.readTree(responseBody);
            String title = json.has("title") ? json.get("title").asText() : fileName;
            String fileId = null;
            String collectionName = null;
            JsonNode extensions = json.get("extensions");
            if (extensions != null) {
                if (extensions.has("fileId")) {
                    fileId = extensions.get("fileId").asText();
                }
                if (extensions.has("collectionName")) {
                    collectionName = extensions.get("collectionName").asText();
                }
            }

            if (fileId == null || collectionName == null) {
                logger.warn("Attachment extensions missing media identifiers (fileId or collectionName). Rendering may fail. attachmentId={} response={}", attachmentId, responseBody);
            } else {
                logger.info("Fetched media identifiers for attachment {} -> fileId={}, collectionName={}", attachmentId, fileId, collectionName);
            }
            return new AttachmentDetails(attachmentId, title, fileId, collectionName);
        } catch (Exception e) {
            throw new IOException("Failed to fetch attachment details", e);
        }
    }

    /** Record containing attachment details and media identifiers. */
    public static record AttachmentDetails(String attachmentId, String filename, String fileId, String collectionName) {}
    
    /**
     * Cleans a page tree by deleting a specific page and all its descendants.
     * 
     * @param pageTitle the title of the root page to clean
     * @throws IOException if the cleanup fails
     */
    public void cleanPageTree(String pageTitle) throws IOException {
        logger.info("Starting cleanup of page tree: {}", pageTitle);
        
        String pageId = findPageByTitle(pageTitle);
        if (pageId == null) {
            logger.info("Page '{}' not found, nothing to clean", pageTitle);
            return;
        }
        
        cleanPageTreeById(pageId);
    }

    /**
     * Cleans a page tree by deleting a specific page and all its descendants using page ID.
     * 
     * @param pageId the ID of the root page to clean
     * @throws IOException if the cleanup fails
     */
    public void cleanPageTreeById(String pageId) throws IOException {
        logger.info("Starting cleanup of page tree for ID: {}", pageId);
        
        // Get all descendant pages
        List<String> descendantIds = getPageDescendants(pageId);
        logger.info("Found {} pages in tree starting from page ID '{}'", descendantIds.size(), pageId);
        
        // Delete all descendants (children first, then the root)
        for (String descendantId : descendantIds) {
            try {
                deletePage(descendantId);
                logger.debug("Deleted page: {}", descendantId);
            } catch (IOException e) {
                logger.warn("Failed to delete page {}, continuing with others", descendantId, e);
            }
        }
        
        // Finally delete the root page
        try {
            deletePage(pageId);
            logger.info("Deleted root page ID: {}", pageId);
        } catch (IOException e) {
            logger.warn("Failed to delete root page {}", pageId, e);
        }
        
        logger.info("Page tree cleanup completed for ID: {}", pageId);
    }

    /**
     * Gets all descendant page IDs for a given page (children, grandchildren, etc.).
     * Returns them in reverse order (deepest first) to allow safe deletion.
     * 
     * @param pageId the root page ID
     * @return list of descendant page IDs in deletion order
     * @throws IOException if the request fails
     */
    private List<String> getPageDescendants(String pageId) throws IOException {
        List<String> allDescendants = new ArrayList<>();
        getPageDescendantsRecursive(pageId, allDescendants);
        
        // Reverse the list so deepest pages are deleted first
        Collections.reverse(allDescendants);
        return allDescendants;
    }

    /**
     * Recursively collects descendant page IDs.
     * 
     * @param pageId the current page ID
     * @param descendants the list to collect descendants into
     * @throws IOException if the request fails
     */
    private void getPageDescendantsRecursive(String pageId, List<String> descendants) throws IOException {
        List<String> children = getChildPages(pageId);
        
        for (String childId : children) {
            descendants.add(childId);
            // Recursively get grandchildren
            getPageDescendantsRecursive(childId, descendants);
        }
    }

    /**
     * Gets direct child pages of a given page.
     * 
     * @param pageId the parent page ID
     * @return list of child page IDs
     * @throws IOException if the request fails
     */
    private List<String> getChildPages(String pageId) throws IOException {
        try {
            String responseBody = api.listChildPages(pageId, 200).await().indefinitely();
            JsonNode responseJson = objectMapper.readTree(responseBody);
                List<String> childIds = new ArrayList<>();
                
                if (responseJson.has("results")) {
                    for (JsonNode child : responseJson.get("results")) {
                        childIds.add(child.get("id").asText());
                    }
                }
                
                return childIds;
        } catch (Exception e) {
            throw new IOException("Failed to get child pages", e);
        }
    }
    
    /**
     * Downloads content from a URL and returns the byte array.
     * 
     * @param url the URL to download from
     * @return the downloaded content as byte array
     * @throws IOException if the download fails
     */
    public byte[] downloadImage(String url) throws IOException {
        try {
            logger.info("Downloading image from: {}", url);
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .header("User-Agent", "Structurizr-Confluence-Exporter/1.0")
                .GET()
                .build();
            var client = SslTrustUtils.shouldDisableSslVerification() 
                ? SslTrustUtils.createTrustAllHttpClient() 
                : HttpClient.newHttpClient();
            HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() == 200) {
                byte[] content = response.body();
                logger.info("Downloaded {} bytes from: {}", content.length, url);
                return content;
            }
            throw new IOException("Failed to download image: HTTP " + response.statusCode());
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted", ie);
        }
    }
}