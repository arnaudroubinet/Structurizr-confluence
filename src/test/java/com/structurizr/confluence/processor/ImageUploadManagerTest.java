package com.structurizr.confluence.processor;

import com.structurizr.confluence.client.ConfluenceClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Test for the new image upload functionality.
 */
class ImageUploadManagerTest {
    private static final Logger logger = LoggerFactory.getLogger(ImageUploadManagerTest.class);
    
    @Mock
    private ConfluenceClient confluenceClient;
    
    private ImageUploadManager imageUploadManager;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        imageUploadManager = new ImageUploadManager(confluenceClient);
    }
    
    @Test
    void testFilenameExtraction() throws Exception {
        logger.info("=== TEST FILENAME EXTRACTION ===");
        
        // Test normal URL with filename
        String url1 = "https://static.structurizr.com/workspace/1/diagrams/context-view.svg";
        String filename1 = imageUploadManager.extractFilenameFromUrl(url1);
        logger.info("URL: {} -> Filename: {}", url1, filename1);
        assertEquals("context-view.svg", filename1);
        
        // Test URL without extension
        String url2 = "https://example.com/image";
        String filename2 = imageUploadManager.extractFilenameFromUrl(url2);
        logger.info("URL: {} -> Filename: {}", url2, filename2);
        assertTrue(filename2.startsWith("image_") && filename2.endsWith(".png"));
        
        // Test URL with query parameters
        String url3 = "https://example.com/diagram.png?version=123&size=large";
        String filename3 = imageUploadManager.extractFilenameFromUrl(url3);
        logger.info("URL: {} -> Filename: {}", url3, filename3);
        assertEquals("diagram.png", filename3); // Query parameters are stripped by URL.getPath()
        
        logger.info("✅ Filename extraction working correctly");
    }
    
    @Test
    void testMimeTypeDetection() throws Exception {
        logger.info("=== TEST MIME TYPE DETECTION ===");
        
        ImageUploadManager manager = new ImageUploadManager(confluenceClient);
        
        // Test various file extensions
        assertEquals("image/png", manager.getMimeTypeFromFilename("diagram.png"));
        assertEquals("image/jpeg", manager.getMimeTypeFromFilename("photo.jpg"));
        assertEquals("image/jpeg", manager.getMimeTypeFromFilename("image.jpeg"));
        assertEquals("image/svg+xml", manager.getMimeTypeFromFilename("icon.svg"));
        assertEquals("image/gif", manager.getMimeTypeFromFilename("animation.gif"));
        assertEquals("image/png", manager.getMimeTypeFromFilename("unknown.xyz")); // fallback
        
        logger.info("✅ MIME type detection working correctly");
    }
    
    @Test
    void testUploadCaching() throws Exception {
        logger.info("=== TEST UPLOAD CACHING ===");
        
        // Mock successful upload
        when(confluenceClient.downloadImage(anyString())).thenReturn(new byte[]{1, 2, 3, 4});
        when(confluenceClient.uploadAttachment(anyString(), anyString(), any(byte[].class), anyString()))
            .thenReturn("attachment-123");
        
        String url = "https://example.com/test.png";
        String pageId = "page-456";
        
        // First call should download and upload
        String filename1 = imageUploadManager.downloadAndUploadImage(url, pageId);
        
        // Second call should use cache
        String filename2 = imageUploadManager.downloadAndUploadImage(url, pageId);
        
        assertEquals(filename1, filename2, "Cached result should be the same");
        
        // Verify download and upload were called only once
        verify(confluenceClient, times(1)).downloadImage(url);
        verify(confluenceClient, times(1)).uploadAttachment(eq(pageId), eq(filename1), any(byte[].class), anyString());
        
        logger.info("✅ Upload caching working correctly");
    }
    
    @Test
    void testImageHandlingWithUploadManager() throws Exception {
        logger.info("=== TEST IMAGE HANDLING WITH UPLOAD MANAGER ===");
        
        // Mock successful upload scenario
        when(confluenceClient.downloadImage(anyString())).thenReturn(new byte[]{1, 2, 3, 4});
        when(confluenceClient.uploadAttachment(anyString(), anyString(), any(byte[].class), anyString()))
            .thenReturn("attachment-123");
        
        HtmlToAdfConverter converter = new HtmlToAdfConverter();
        ImageUploadManager uploadManager = new ImageUploadManager(confluenceClient);
        
        converter.setImageUploadManager(uploadManager);
        converter.setCurrentPageId("test-page-123");
        
        // Test external image with Structurizr diagram URL
        String htmlWithExternalImage = "<img src=\"https://static.structurizr.com/workspace/1/diagrams/context-view.svg\" alt=\"Context Diagram\">";
        
        com.atlassian.adf.Document doc = converter.convertToAdf(htmlWithExternalImage, "Image Test");
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        String adfJson = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(doc);
        
        logger.info("ADF JSON: {}", adfJson);
        
        // Verify that it creates a file media node (not link) since image was uploaded
        assertTrue(adfJson.contains("\"type\" : \"mediaGroup\""), "Should create mediaGroup");
        assertTrue(adfJson.contains("\"type\" : \"file\""), "External image should be uploaded and referenced as file");
        assertTrue(adfJson.contains("context-view.svg"), "Should preserve original filename");
        assertFalse(adfJson.contains("\"type\" : \"link\""), "Should NOT use external link type");
        
        // Verify upload was called
        verify(confluenceClient, times(1)).downloadImage("https://static.structurizr.com/workspace/1/diagrams/context-view.svg");
        verify(confluenceClient, times(1)).uploadAttachment(eq("test-page-123"), eq("context-view.svg"), any(byte[].class), eq("image/svg+xml"));
        
        logger.info("✅ Image handling with upload manager working correctly");
    }
}