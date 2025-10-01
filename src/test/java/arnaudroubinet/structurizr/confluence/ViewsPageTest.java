package arnaudroubinet.structurizr.confluence;

import com.atlassian.adf.Document;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test for Views page document building logic.
 * This test verifies that the document combination logic works correctly
 * for building up the Views page with multiple diagrams.
 */
public class ViewsPageTest {
    private static final Logger logger = LoggerFactory.getLogger(ViewsPageTest.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Test
    @DisplayName("Should create non-empty document when combining multiple documents")
    public void testCombineDocuments() throws Exception {
        // Create initial empty document (similar to addExportedDiagrams)
        Document baseDoc = Document.create();
        
        logger.info("Initial document: {}", objectMapper.writeValueAsString(baseDoc));
        
        // Create a simple paragraph document to add
        Document addDoc = Document.create()
            .paragraph("Test paragraph 1");
        
        logger.info("Document to add: {}", objectMapper.writeValueAsString(addDoc));
        
        // Combine documents (simulate what happens in addExportedDiagrams loop)
        Document combined = combineDocuments(baseDoc, addDoc);
        
        logger.info("Combined document: {}", objectMapper.writeValueAsString(combined));
        
        String combinedJson = objectMapper.writeValueAsString(combined);
        
        // Verify the combined document is not empty
        assertNotNull(combined, "Combined document should not be null");
        assertTrue(combinedJson.contains("Test paragraph 1"), 
            "Combined document should contain the added paragraph");
    }
    
    @Test
    @DisplayName("Should combine multiple documents in a loop")
    public void testCombineMultipleDocumentsInLoop() throws Exception {
        // Create initial empty document
        Document doc = Document.create();
        
        logger.info("Initial document: {}", objectMapper.writeValueAsString(doc));
        
        // Simulate the loop in addExportedDiagrams
        for (int i = 1; i <= 3; i++) {
            Document imgDoc = Document.create()
                .paragraph("Diagram " + i);
            
            logger.info("Adding document {}: {}", i, objectMapper.writeValueAsString(imgDoc));
            
            doc = combineDocuments(doc, imgDoc);
            
            logger.info("Document after adding {}: {}", i, objectMapper.writeValueAsString(doc));
        }
        
        String finalJson = objectMapper.writeValueAsString(doc);
        logger.info("Final combined document: {}", finalJson);
        
        // Verify all paragraphs are in the final document
        assertTrue(finalJson.contains("Diagram 1"), "Should contain Diagram 1");
        assertTrue(finalJson.contains("Diagram 2"), "Should contain Diagram 2");
        assertTrue(finalJson.contains("Diagram 3"), "Should contain Diagram 3");
    }
    
    /**
     * Duplicates the combineDocuments logic from ConfluenceExporter for testing.
     */
    private Document combineDocuments(Document base, Document addition) {
        try {
            com.fasterxml.jackson.databind.node.ObjectNode baseNode = objectMapper.valueToTree(base);
            com.fasterxml.jackson.databind.node.ObjectNode addNode = objectMapper.valueToTree(addition);

            com.fasterxml.jackson.databind.node.ArrayNode baseContent;
            com.fasterxml.jackson.databind.JsonNode baseContentNode = baseNode.get("content");
            if (baseContentNode != null && baseContentNode.isArray()) {
                baseContent = (com.fasterxml.jackson.databind.node.ArrayNode) baseContentNode;
            } else {
                baseContent = baseNode.putArray("content");
            }

            com.fasterxml.jackson.databind.JsonNode addContentNode = addNode.get("content");
            if (addContentNode != null && addContentNode.isArray()) {
                for (com.fasterxml.jackson.databind.JsonNode child : addContentNode) {
                    baseContent.add(child);
                }
            }

            return objectMapper.treeToValue(baseNode, Document.class);
        } catch (Exception e) {
            logger.warn("Failed to merge ADF documents, keeping base content only", e);
            return base;
        }
    }
}
