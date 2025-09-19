package com.structurizr.confluence.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Client for interacting with Confluence Cloud REST API.
 */
public class ConfluenceClient {
    
    private static final Logger logger = LoggerFactory.getLogger(ConfluenceClient.class);
    
    private final ConfluenceConfig config;
    private final ObjectMapper objectMapper;
    private final String authHeader;
    
    public ConfluenceClient(ConfluenceConfig config) {
        this.config = config;
        this.objectMapper = new ObjectMapper();
        this.authHeader = "Basic " + Base64.getEncoder().encodeToString(
            (config.getUsername() + ":" + config.getApiToken()).getBytes(StandardCharsets.UTF_8));
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
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            try {
                URIBuilder uriBuilder = new URIBuilder(config.getBaseUrl() + "/wiki/rest/api/content");
                uriBuilder.addParameter("title", title);
                uriBuilder.addParameter("spaceKey", config.getSpaceKey());
                HttpGet httpGet = new HttpGet(uriBuilder.build());
            httpGet.setHeader("Authorization", authHeader);
            httpGet.setHeader("Accept", "application/json");
            
            try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
                String responseBody = EntityUtils.toString(response.getEntity());
                JsonNode responseJson = objectMapper.readTree(responseBody);
                
                if (responseJson.has("results") && responseJson.get("results").size() > 0) {
                    return responseJson.get("results").get(0).get("id").asText();
                }
            }
            } catch (Exception e) {
                throw new IOException("Failed to build Confluence content search URI", e);
            }
        }
        return null;
    }
    
    private String createPage(String title, String adfContent, String parentId) throws IOException {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            String spaceId = getSpaceId();
            String url = config.getBaseUrl() + "/wiki/api/v2/pages";
            HttpPost httpPost = new HttpPost(url);
            httpPost.setHeader("Authorization", authHeader);
            httpPost.setHeader("Content-Type", "application/json; charset=UTF-8");
            
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
            httpPost.setEntity(new StringEntity(jsonBody, StandardCharsets.UTF_8));
            
            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                String responseBody = EntityUtils.toString(response.getEntity());
                if (response.getStatusLine().getStatusCode() == 200) {
                    JsonNode responseJson = objectMapper.readTree(responseBody);
                    String pageId = responseJson.get("id").asText();
                    logger.info("Page created successfully with ID: {}", pageId);
                    return pageId;
                } else {
                    logger.error("Failed to create page. Status: {}, Response: {}", 
                        response.getStatusLine().getStatusCode(), responseBody);
                    throw new IOException("Failed to create page: " + responseBody);
                }
            }
        }
    }
    
    private String updatePage(String pageId, String title, String adfContent) throws IOException {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            // Get current page version using API v2
            String getUrl = config.getBaseUrl() + "/wiki/api/v2/pages/" + pageId;
            HttpGet httpGet = new HttpGet(getUrl);
            httpGet.setHeader("Authorization", authHeader);
            httpGet.setHeader("Accept", "application/json");
            
            int currentVersion;
            String spaceId;
            String parentId = null;
            try (CloseableHttpResponse getResponse = httpClient.execute(httpGet)) {
                String getResponseBody = EntityUtils.toString(getResponse.getEntity());
                
                // Check if the response contains an error
                if (getResponse.getStatusLine().getStatusCode() != 200) {
                    throw new IOException("Failed to get page details: " + getResponse.getStatusLine().getStatusCode() + " - " + getResponseBody);
                }
                
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
            }
            
            // Update page using API v2
            String putUrl = config.getBaseUrl() + "/wiki/api/v2/pages/" + pageId;
            HttpPut httpPut = new HttpPut(putUrl);
            httpPut.setHeader("Authorization", authHeader);
            httpPut.setHeader("Content-Type", "application/json; charset=UTF-8");
            
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
            httpPut.setEntity(new StringEntity(jsonBody, StandardCharsets.UTF_8));
            
            try (CloseableHttpResponse response = httpClient.execute(httpPut)) {
                String responseBody = EntityUtils.toString(response.getEntity());
                if (response.getStatusLine().getStatusCode() == 200) {
                    logger.info("Page updated successfully with ID: {}", pageId);
                    return pageId;
                } else {
                    logger.error("Failed to update page. Status: {}, Response: {}", 
                        response.getStatusLine().getStatusCode(), responseBody);
                    throw new IOException("Failed to update page: " + responseBody);
                }
            }
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
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            // Utiliser l'endpoint API v2 pour lister les espaces et filtrer par clé
            HttpGet httpGet = new HttpGet(config.getBaseUrl() + "/wiki/api/v2/spaces?keys=" + config.getSpaceKey());
            httpGet.setHeader("Authorization", authHeader);
            httpGet.setHeader("Accept", "application/json");
            
            logger.info("Getting space ID for space key: {}", config.getSpaceKey());
            
            try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
                String responseBody = EntityUtils.toString(response.getEntity());
                
                if (response.getStatusLine().getStatusCode() != 200) {
                    throw new IOException("Failed to get space ID: " + response.getStatusLine().getStatusCode() + " - " + responseBody);
                }
                
                JsonNode jsonResponse = objectMapper.readTree(responseBody);
                JsonNode results = jsonResponse.get("results");
                
                if (results == null || results.isEmpty()) {
                    throw new IOException("Space not found with key: " + config.getSpaceKey());
                }
                
                String spaceId = results.get(0).get("id").asText();
                logger.info("Found space ID: {} for key: {}", spaceId, config.getSpaceKey());
                return spaceId;
            }
        } catch (Exception e) {
            throw new IOException("Error getting space ID", e);
        }
    }

    /**
     * Lists all pages in the specified space using API v2.
     * 
     * @return an array of page IDs in the space
     * @throws IOException if the request fails
     */
    public String[] listPagesInSpace() throws IOException {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            String spaceId = getSpaceId();
            
            // Utiliser l'endpoint API v2 avec l'ID de l'espace
            HttpGet httpGet = new HttpGet(config.getBaseUrl() + "/wiki/api/v2/spaces/" + spaceId + "/pages");
            httpGet.setHeader("Authorization", authHeader);
            httpGet.setHeader("Accept", "application/json");
            
            logger.info("Listing pages in space: {} (ID: {})", config.getSpaceKey(), spaceId);
            
            try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
                String responseBody = EntityUtils.toString(response.getEntity());
                
                if (response.getStatusLine().getStatusCode() == 404) {
                    logger.info("Space '{}' not found or no pages exist", config.getSpaceKey());
                    return new String[0]; // Retourner un tableau vide si l'espace n'existe pas
                }
                
                if (response.getStatusLine().getStatusCode() != 200) {
                    throw new IOException("Failed to list pages in space: " + response.getStatusLine().getStatusCode() + " - " + responseBody);
                }
                
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
            }
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
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            // Utiliser l'endpoint API v2 pour supprimer une page
            HttpDelete httpDelete = new HttpDelete(config.getBaseUrl() + "/wiki/api/v2/pages/" + pageId);
            httpDelete.setHeader("Authorization", authHeader);
            
            logger.info("Deleting page with ID: {}", pageId);
            
            try (CloseableHttpResponse response = httpClient.execute(httpDelete)) {
                if (response.getStatusLine().getStatusCode() != 204) {
                    String responseBody = EntityUtils.toString(response.getEntity());
                    logger.warn("Failed to delete page {}: {} - {}", pageId, response.getStatusLine().getStatusCode(), responseBody);
                } else {
                    logger.info("Successfully deleted page: {}", pageId);
                }
            }
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
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            String url = config.getBaseUrl() + "/wiki/rest/api/content/" + pageId + "/child/attachment";
            
            HttpPost httpPost = new HttpPost(url);
            httpPost.setHeader("Authorization", authHeader);
            httpPost.setHeader("X-Atlassian-Token", "nocheck"); // Required for multipart uploads
            
            // Create multipart entity
            org.apache.http.entity.mime.MultipartEntityBuilder builder = 
                org.apache.http.entity.mime.MultipartEntityBuilder.create();
            builder.addBinaryBody("file", fileContent, 
                org.apache.http.entity.ContentType.create(mimeType), fileName);
            
            HttpEntity multipart = builder.build();
            httpPost.setEntity(multipart);
            
            logger.info("Uploading attachment '{}' to page ID: {}", fileName, pageId);
            
            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                String responseBody = EntityUtils.toString(response.getEntity());
                
                if (response.getStatusLine().getStatusCode() == 200) {
                    JsonNode jsonResponse = objectMapper.readTree(responseBody);
                    JsonNode results = jsonResponse.get("results");
                    if (results != null && results.isArray() && results.size() > 0) {
                        String attachmentId = results.get(0).get("id").asText();
                        logger.info("Attachment uploaded successfully with ID: {}", attachmentId);
                        return attachmentId;
                    } else {
                        throw new IOException("Upload successful but no attachment ID returned");
                    }
                } else {
                    logger.error("Failed to upload attachment. Status: {}, Response: {}", 
                        response.getStatusLine().getStatusCode(), responseBody);
                    throw new IOException("Failed to upload attachment: " + responseBody);
                }
            }
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
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet httpGet = new HttpGet(url);
            httpGet.setHeader("User-Agent", "Structurizr-Confluence-Exporter/1.0");
            
            logger.info("Downloading image from: {}", url);
            
            try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
                if (response.getStatusLine().getStatusCode() == 200) {
                    byte[] content = EntityUtils.toByteArray(response.getEntity());
                    logger.info("Downloaded {} bytes from: {}", content.length, url);
                    return content;
                } else {
                    throw new IOException("Failed to download image: " + response.getStatusLine().getStatusCode());
                }
            }
        }
    }
}