package com.structurizr.confluence.processor;

import com.atlassian.adf.Document;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test to validate improved image handling in HTML to ADF conversion.
 */
class ImageHandlingTest {
    private static final Logger logger = LoggerFactory.getLogger(ImageHandlingTest.class);
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HtmlToAdfConverter converter = new HtmlToAdfConverter();
    
    @Test
    void testExternalImageHandling() throws Exception {
        logger.info("=== TEST EXTERNAL IMAGE HANDLING ===");
        
        // Test external image with Structurizr diagram URL
        String htmlWithExternalImage = "<img src=\"https://structurizr.roubinet.fr/workspace/1/diagrams/itms-context.svg\" alt=\"Context Diagram\" title=\"ITMS Context View\">";
        
        Document doc = converter.convertToAdf(htmlWithExternalImage, "Image Test");
        String adfJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(doc);
        
        logger.info("HTML: {}", htmlWithExternalImage);
        logger.info("ADF: {}", adfJson);
        
        // Verify that it creates a native media node instead of text
        assertTrue(adfJson.contains("\"type\" : \"mediaGroup\""), "Should create mediaGroup for external image");
        assertTrue(adfJson.contains("\"type\" : \"media\""), "Should create media node inside mediaGroup");
        assertTrue(adfJson.contains("\"type\" : \"file\""), "External image should be uploaded as attachment");
        // URL might be different as it's uploaded as attachment, but should have the image name
        assertTrue(adfJson.contains("itms-context.svg") || adfJson.contains("attachment"), "Should reference uploaded image");
        assertTrue(adfJson.contains("\"occurrenceKey\" : \"ITMS Context View\""), "Should use title as caption");
        
        // Verify it's NOT using the old text format
        assertFalse(adfJson.contains("Image: https://"), "Should NOT use text fallback format");
        
        logger.info("✅ External image correctly converted to native ADF media node");
    }
    
    @Test
    void testLocalImageHandling() throws Exception {
        logger.info("=== TEST LOCAL IMAGE HANDLING ===");
        
        // Test local/attached image
        String htmlWithLocalImage = "<img src=\"diagram.png\" alt=\"Architecture Diagram\">";
        
        Document doc = converter.convertToAdf(htmlWithLocalImage, "Local Image Test");
        String adfJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(doc);
        
        logger.info("HTML: {}", htmlWithLocalImage);
        logger.info("ADF: {}", adfJson);
        
        // Verify that it creates a native media node for local file
        assertTrue(adfJson.contains("\"type\" : \"mediaGroup\""), "Should create mediaGroup for local image");
        assertTrue(adfJson.contains("\"type\" : \"media\""), "Should create media node inside mediaGroup");
        assertTrue(adfJson.contains("\"type\" : \"file\""), "Local image should use file type");
        assertTrue(adfJson.contains("\"collection\" : \"diagram.png\""), "Should preserve filename");
        assertTrue(adfJson.contains("\"occurrenceKey\" : \"Architecture Diagram\""), "Should use alt as caption");
        
        logger.info("✅ Local image correctly converted to native ADF media node");
    }
    
    @Test
    void testImageWithoutCaptionHandling() throws Exception {
        logger.info("=== TEST IMAGE WITHOUT CAPTION ===");
        
        // Test image without alt or title
        String htmlWithoutCaption = "<img src=\"https://example.com/image.jpg\">";
        
        Document doc = converter.convertToAdf(htmlWithoutCaption, "No Caption Test");
        String adfJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(doc);
        
        logger.info("HTML: {}", htmlWithoutCaption);
        logger.info("ADF: {}", adfJson);
        
        // Should still create media node but without occurrenceKey
        assertTrue(adfJson.contains("\"type\" : \"mediaGroup\""), "Should create mediaGroup even without caption");
        assertTrue(adfJson.contains("\"type\" : \"media\""), "Should create media node");
        assertFalse(adfJson.contains("\"occurrenceKey\""), "Should NOT have occurrenceKey when no caption");
        
        logger.info("✅ Image without caption correctly handled");
    }
    
    @Test
    void testStructurizrDiagramEmbedWorkflow() throws Exception {
        logger.info("=== TEST STRUCTURIZR DIAGRAM EMBED WORKFLOW ===");
        
        // Test the complete workflow: AsciiDoc -> HTML -> ADF for Structurizr diagrams
        AsciiDocConverter asciiConverter = new AsciiDocConverter();
        
        String asciiDocWithEmbed = "== Architecture Overview\n\nimage::https://structurizr.roubinet.fr/workspace/1/diagrams/context-view.svg[Context View, title=System Context Diagram]";
        
        // Step 1: AsciiDoc to HTML
        String html = asciiConverter.convertToHtml(asciiDocWithEmbed, "diagram_test");
        logger.info("AsciiDoc to HTML: {}", html);
        
        // Step 2: HTML to ADF
        Document doc = converter.convertToAdf(html, "Structurizr Diagram Test");
        String adfJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(doc);
        
        logger.info("Final ADF: {}", adfJson);
        
        // Verify complete preservation
        assertTrue(adfJson.contains("\"type\" : \"mediaGroup\""), "Should preserve Structurizr diagram as media");
        assertTrue(adfJson.contains("\"type\" : \"file\""), "Structurizr diagram should be uploaded as attachment");
        assertTrue(adfJson.contains("context-view.svg") || adfJson.contains("attachment"), "Should reference uploaded diagram");
        
        logger.info("✅ Complete Structurizr diagram workflow preserved formatting");
    }
}