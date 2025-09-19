package com.structurizr.confluence.processor;

import com.atlassian.adf.Document;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test to explore ADF Builder image/media capabilities.
 */
class AdfImageExplorationTest {
    private static final Logger logger = LoggerFactory.getLogger(AdfImageExplorationTest.class);
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Test
    void exploreImageSupport() throws Exception {
        logger.info("=== EXPLORATION SUPPORT IMAGE ADF ===");
        
        try {
            // Test mediaGroup with link method for external images (id, url)
            logger.info("Testing mediaGroup with link method...");
            
            Document docWithImageLink = Document.create()
                .h1("Test with External Image")
                .mediaGroup(mediaGroup -> {
                    mediaGroup.link("image-id", "https://static.structurizr.com/workspace/1/diagrams/itms-context.png");
                });
            
            String adfJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(docWithImageLink);
            logger.info("ADF with external image link:");
            logger.info(adfJson);
            
            // Test mediaGroup with file method for attachments (id, filename)
            logger.info("Testing mediaGroup with file method...");
            
            Document docWithImageFile = Document.create()
                .h1("Test with Attached Image")
                .mediaGroup(mediaGroup -> {
                    mediaGroup.file("attachment-id", "diagram.png");
                });
            
            String adfJson2 = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(docWithImageFile);
            logger.info("ADF with attached image file:");
            logger.info(adfJson2);
            
            // Test with 3 parameters - check available overloads
            logger.info("Testing mediaGroup with 3 parameters...");
            
            Document docWithImageFile3 = Document.create()
                .h1("Test with 3 params")
                .mediaGroup(mediaGroup -> {
                    mediaGroup.file("attachment-id", "diagram.png", "Image caption");
                });
            
            String adfJson3 = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(docWithImageFile3);
            logger.info("ADF with 3 params:");
            logger.info(adfJson3);
            
        } catch (Exception e) {
            logger.error("Error exploring ADF image support", e);
        }
    }
}