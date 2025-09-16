package com.structurizr.confluence;

import com.atlassian.adf.Document;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test for the official ADF library integration.
 */
class AdfIntegrationTest {
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Test
    void shouldCreateAdfDocumentWithOfficialLibrary() throws Exception {
        Document doc = Document.create()
            .h1("Test Title")
            .paragraph("This is a test paragraph.")
            .bulletList(list -> list
                .item("First item")
                .item("Second item"));
        
        String json = objectMapper.writeValueAsString(doc);
        
        assertTrue(json.contains("\"version\":1"));
        assertTrue(json.contains("\"type\":\"doc\""));
        assertTrue(json.contains("Test Title"));
        assertTrue(json.contains("This is a test paragraph"));
        assertTrue(json.contains("First item"));
        assertTrue(json.contains("Second item"));
        assertTrue(json.contains("bulletList"));
    }
    
    @Test
    void shouldCreateComplexDocument() throws Exception {
        Document doc = Document.create()
            .h1("Architecture Documentation")
            .paragraph("This document describes the system architecture.")
            .h2("Components")
            .bulletList(list -> list
                .item("Web Frontend")
                .item("API Gateway")
                .item("Database"))
            .h3("Details")
            .paragraph("Each component has specific responsibilities.");
        
        String json = objectMapper.writeValueAsString(doc);
        
        assertTrue(json.contains("Architecture Documentation"));
        assertTrue(json.contains("Components"));
        assertTrue(json.contains("Web Frontend"));
        assertTrue(json.contains("API Gateway"));
        assertFalse(json.isEmpty());
    }
}