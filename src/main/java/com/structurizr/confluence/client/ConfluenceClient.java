package com.structurizr.confluence.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.structurizr.confluence.adf.AdfDocument;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
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
            (config.getUsername() + ":" + config.getApiToken()).getBytes());
    }
    
    /**
     * Creates or updates a page in Confluence with ADF content.
     */
    public String createOrUpdatePage(String title, AdfDocument content) throws IOException {
        return createOrUpdatePage(title, content, null);
    }
    
    /**
     * Creates or updates a page in Confluence with ADF content under a specific parent.
     */
    public String createOrUpdatePage(String title, AdfDocument content, String parentId) throws IOException {
        // First, check if page exists
        String existingPageId = findPageByTitle(title);
        
        if (existingPageId != null) {
            return updatePage(existingPageId, title, content);
        } else {
            return createPage(title, content, parentId);
        }
    }
    
    private String findPageByTitle(String title) throws IOException {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            String url = config.getBaseUrl() + "/wiki/rest/api/content?title=" + title + "&spaceKey=" + config.getSpaceKey();
            HttpGet httpGet = new HttpGet(url);
            httpGet.setHeader("Authorization", authHeader);
            httpGet.setHeader("Accept", "application/json");
            
            try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
                String responseBody = EntityUtils.toString(response.getEntity());
                JsonNode responseJson = objectMapper.readTree(responseBody);
                
                if (responseJson.has("results") && responseJson.get("results").size() > 0) {
                    return responseJson.get("results").get(0).get("id").asText();
                }
            }
        }
        return null;
    }
    
    private String createPage(String title, AdfDocument content, String parentId) throws IOException {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            String url = config.getBaseUrl() + "/wiki/rest/api/content";
            HttpPost httpPost = new HttpPost(url);
            httpPost.setHeader("Authorization", authHeader);
            httpPost.setHeader("Content-Type", "application/json");
            
            Map<String, Object> pageData = new HashMap<>();
            pageData.put("type", "page");
            pageData.put("title", title);
            
            Map<String, Object> space = new HashMap<>();
            space.put("key", config.getSpaceKey());
            pageData.put("space", space);
            
            if (parentId != null) {
                Map<String, Object> parent = new HashMap<>();
                parent.put("id", parentId);
                pageData.put("ancestors", new Object[]{parent});
            }
            
            Map<String, Object> body = new HashMap<>();
            Map<String, Object> atlas = new HashMap<>();
            atlas.put("value", objectMapper.writeValueAsString(content));
            atlas.put("representation", "atlas_doc_format");
            body.put("atlas_doc_format", atlas);
            pageData.put("body", body);
            
            String jsonBody = objectMapper.writeValueAsString(pageData);
            httpPost.setEntity(new StringEntity(jsonBody));
            
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
    
    private String updatePage(String pageId, String title, AdfDocument content) throws IOException {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            // Get current page version
            String getUrl = config.getBaseUrl() + "/wiki/rest/api/content/" + pageId;
            HttpGet httpGet = new HttpGet(getUrl);
            httpGet.setHeader("Authorization", authHeader);
            httpGet.setHeader("Accept", "application/json");
            
            int currentVersion;
            try (CloseableHttpResponse getResponse = httpClient.execute(httpGet)) {
                String getResponseBody = EntityUtils.toString(getResponse.getEntity());
                JsonNode getResponseJson = objectMapper.readTree(getResponseBody);
                currentVersion = getResponseJson.get("version").get("number").asInt();
            }
            
            // Update page
            String putUrl = config.getBaseUrl() + "/wiki/rest/api/content/" + pageId;
            HttpPut httpPut = new HttpPut(putUrl);
            httpPut.setHeader("Authorization", authHeader);
            httpPut.setHeader("Content-Type", "application/json");
            
            Map<String, Object> pageData = new HashMap<>();
            pageData.put("id", pageId);
            pageData.put("type", "page");
            pageData.put("title", title);
            
            Map<String, Object> space = new HashMap<>();
            space.put("key", config.getSpaceKey());
            pageData.put("space", space);
            
            Map<String, Object> version = new HashMap<>();
            version.put("number", currentVersion + 1);
            pageData.put("version", version);
            
            Map<String, Object> body = new HashMap<>();
            Map<String, Object> atlas = new HashMap<>();
            atlas.put("value", objectMapper.writeValueAsString(content));
            atlas.put("representation", "atlas_doc_format");
            body.put("atlas_doc_format", atlas);
            pageData.put("body", body);
            
            String jsonBody = objectMapper.writeValueAsString(pageData);
            httpPut.setEntity(new StringEntity(jsonBody));
            
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
}