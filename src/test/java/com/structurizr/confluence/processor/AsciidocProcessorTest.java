package com.structurizr.confluence.processor;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for AsciiDoc processing functionality.
 */
class AsciidocProcessorTest {
    
    private AsciidocProcessor processor;
    
    @BeforeEach
    void setUp() {
        processor = new AsciidocProcessor();
    }
    
    @AfterEach
    void tearDown() {
        if (processor != null) {
            processor.close();
        }
    }
    
    @Test
    void shouldProcessSimpleAsciidocContent() {
        String asciidocContent = "= Document Title\n" +
            "\n" +
            "== Section 1\n" +
            "\n" +
            "This is a paragraph with *bold* text and _italic_ text.\n" +
            "\n" +
            "== Section 2\n" +
            "\n" +
            "Another section with content.";
        
        String htmlContent = processor.processAsciidocContent(asciidocContent);
        
        assertNotNull(htmlContent);
        assertFalse(htmlContent.trim().isEmpty());
        assertTrue(htmlContent.contains("Document Title"));
        assertTrue(htmlContent.contains("Section 1"));
        assertTrue(htmlContent.contains("Section 2"));
        assertTrue(htmlContent.contains("<strong>bold</strong>") || htmlContent.contains("<b>bold</b>"));
    }
    
    @Test
    void shouldExtractSectionsFromHtml() {
        String htmlContent = "<div class=\"sect1\">\n" +
            "<h2 id=\"introduction\">Introduction</h2>\n" +
            "<p>This is the introduction section.</p>\n" +
            "</div>\n" +
            "<div class=\"sect1\">\n" +
            "<h2 id=\"overview\">System Overview</h2>\n" +
            "<p>This is the overview section.</p>\n" +
            "</div>";
        
        Map<String, String> sections = processor.extractSections(htmlContent);
        
        assertNotNull(sections);
        assertEquals(2, sections.size());
        assertTrue(sections.containsKey("Introduction"));
        assertTrue(sections.containsKey("System Overview"));
        assertTrue(sections.get("Introduction").contains("introduction section"));
        assertTrue(sections.get("System Overview").contains("overview section"));
    }
    
    @Test
    void shouldProcessAsciidocResourceFile() throws Exception {
        String htmlContent = processor.processAsciidocResource("/financial-risk-system.adoc");
        
        assertNotNull(htmlContent);
        assertFalse(htmlContent.trim().isEmpty());
        assertTrue(htmlContent.contains("Financial Risk System"));
        assertTrue(htmlContent.contains("Architecture Documentation"));
    }
    
    @Test
    void shouldHandleAsciidocWithDiagrams() {
        String asciidocContent = "= Document with Diagrams\n" +
            "\n" +
            "== Introduction\n" +
            "\n" +
            "This document contains PlantUML diagrams.\n" +
            "\n" +
            "[plantuml]\n" +
            "....\n" +
            "@startuml\n" +
            "Alice -> Bob: Hello\n" +
            "@enduml\n" +
            "....\n" +
            "\n" +
            "== Conclusion\n" +
            "\n" +
            "End of document.";
        
        String htmlContent = processor.processAsciidocContent(asciidocContent);
        
        assertNotNull(htmlContent);
        assertFalse(htmlContent.trim().isEmpty());
        assertTrue(htmlContent.contains("Document with Diagrams"));
        assertTrue(htmlContent.contains("Introduction"));
        assertTrue(htmlContent.contains("Conclusion"));
        // Note: PlantUML processing might not work in test environment without proper setup
    }
    
    @Test
    void shouldProcessSection() {
        String sectionContent = "This is content for a specific section with *formatting*.";
        
        String htmlContent = processor.processSection("Test Section", sectionContent);
        
        assertNotNull(htmlContent);
        assertFalse(htmlContent.trim().isEmpty());
        assertTrue(htmlContent.contains("Test Section"));
        assertTrue(htmlContent.contains("specific section"));
    }
    
    @Test
    void shouldHandleEmptyContent() {
        String htmlContent = processor.processAsciidocContent("");
        
        assertNotNull(htmlContent);
        // Empty content should still return valid HTML (even if minimal)
    }
    
    @Test
    void shouldHandleNullContent() {
        assertThrows(Exception.class, () -> {
            processor.processAsciidocContent(null);
        });
    }
}