package com.structurizr.confluence.adf;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AdfDocumentTest {
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Test
    void shouldCreateBasicDocument() {
        AdfDocument doc = new AdfDocument();
        
        assertEquals(1, doc.getVersion());
        assertEquals("doc", doc.getType());
        assertNotNull(doc.getContent());
        assertTrue(doc.getContent().isEmpty());
    }
    
    @Test
    void shouldAddContentToDocument() {
        AdfDocument doc = new AdfDocument();
        AdfNode heading = AdfNode.heading(1).addContent(AdfNode.text("Test Title"));
        
        doc.addContent(heading);
        
        assertEquals(1, doc.getContent().size());
        assertEquals("heading", doc.getContent().get(0).getType());
    }
    
    @Test
    void shouldSerializeToJson() throws Exception {
        AdfDocument doc = new AdfDocument();
        doc.addContent(AdfNode.heading(1)
            .addContent(AdfNode.text("Test Title")));
        doc.addContent(AdfNode.paragraph()
            .addContent(AdfNode.text("Test paragraph content.")));
        
        String json = objectMapper.writeValueAsString(doc);
        
        assertTrue(json.contains("\"version\":1"));
        assertTrue(json.contains("\"type\":\"doc\""));
        assertTrue(json.contains("\"content\""));
        assertTrue(json.contains("Test Title"));
        assertTrue(json.contains("Test paragraph content"));
    }
}