package com.structurizr.confluence.processor;

import org.asciidoctor.Asciidoctor;
import org.asciidoctor.Attributes;
import org.asciidoctor.Options;
import org.asciidoctor.SafeMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Processes AsciiDoc documentation with diagram injection capabilities.
 * Converts AsciiDoc content to HTML and processes embedded PlantUML diagrams.
 */
public class AsciidocProcessor {
    
    private static final Logger logger = LoggerFactory.getLogger(AsciidocProcessor.class);
    
    private final Asciidoctor asciidoctor;
    
    public AsciidocProcessor() {
        this.asciidoctor = Asciidoctor.Factory.create();
        // Enable diagram support
        this.asciidoctor.requireLibrary("asciidoctor-diagram");
    }
    
    /**
     * Processes AsciiDoc content from a resource file and converts it to HTML.
     * 
     * @param resourcePath the path to the AsciiDoc resource file
     * @return processed HTML content
     * @throws IOException if the resource cannot be read
     */
    public String processAsciidocResource(String resourcePath) throws IOException {
        logger.info("Processing AsciiDoc resource: {}", resourcePath);
        
        try (InputStream inputStream = getClass().getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                throw new IOException("Resource not found: " + resourcePath);
            }
            
            String content = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            return processAsciidocContent(content);
        }
    }
    
    /**
     * Processes AsciiDoc content and converts it to HTML.
     * 
     * @param asciidocContent the AsciiDoc content to process
     * @return processed HTML content
     */
    public String processAsciidocContent(String asciidocContent) {
        logger.info("Processing AsciiDoc content ({} characters)", asciidocContent.length());
        
        // Configure options for processing
        Options options = Options.builder()
                .safe(SafeMode.UNSAFE) // Allow includes and macro processing
                .backend("html5")
                .headerFooter(false) // Don't include HTML document structure
                .attributes(createAttributes())
                .build();
        
        try {
            String htmlContent = asciidoctor.convert(asciidocContent, options);
            logger.info("Successfully processed AsciiDoc content to HTML ({} characters)", htmlContent.length());
            return htmlContent;
        } catch (Exception e) {
            logger.error("Error processing AsciiDoc content", e);
            throw new RuntimeException("Failed to process AsciiDoc content", e);
        }
    }
    
    /**
     * Extracts document sections from processed HTML for structured export.
     * 
     * @param htmlContent the processed HTML content
     * @return map of section titles to content
     */
    public Map<String, String> extractSections(String htmlContent) {
        Map<String, String> sections = new HashMap<>();
        
        // Simple section extraction based on h2 headers
        String[] parts = htmlContent.split("<h2[^>]*>");
        
        if (parts.length > 1) {
            // First part is usually the introduction
            if (!parts[0].trim().isEmpty()) {
                sections.put("Introduction", parts[0].trim());
            }
            
            for (int i = 1; i < parts.length; i++) {
                String part = parts[i];
                int headerEnd = part.indexOf("</h2>");
                if (headerEnd > 0) {
                    String title = part.substring(0, headerEnd).replaceAll("<[^>]*>", "").trim();
                    String content = part.substring(headerEnd + 5).trim();
                    
                    // Clean up section title
                    title = title.replaceAll("^\\d+\\.\\s*", ""); // Remove numbering
                    
                    sections.put(title, content);
                }
            }
        } else {
            // No sections found, treat as single content
            sections.put("Content", htmlContent);
        }
        
        logger.info("Extracted {} sections from HTML content", sections.size());
        return sections;
    }
    
    /**
     * Processes a specific section of AsciiDoc content.
     * 
     * @param sectionTitle the title of the section
     * @param sectionContent the AsciiDoc content of the section
     * @return processed HTML content for the section
     */
    public String processSection(String sectionTitle, String sectionContent) {
        logger.debug("Processing section: {}", sectionTitle);
        
        // Add section header if not present
        String content = sectionContent;
        if (!content.startsWith("=")) {
            content = "== " + sectionTitle + "\n\n" + content;
        }
        
        return processAsciidocContent(content);
    }
    
    /**
     * Creates attributes for AsciiDoc processing with diagram support.
     * 
     * @return configured attributes
     */
    private Attributes createAttributes() {
        Map<String, Object> attributeMap = new HashMap<>();
        
        // Enable diagrams
        attributeMap.put("plantuml-format", "svg");
        attributeMap.put("diagram-cachedir", "target/diagrams");
        attributeMap.put("imagesdir", "images");
        
        // Document attributes
        attributeMap.put("sectanchors", true);
        attributeMap.put("sectlinks", true);
        attributeMap.put("sectnums", true);
        attributeMap.put("toc", "left");
        attributeMap.put("toclevels", 3);
        
        // Source highlighting
        attributeMap.put("source-highlighter", "coderay");
        
        return Attributes.builder()
                .attributes(attributeMap)
                .build();
    }
    
    /**
     * Cleans up resources used by the processor.
     */
    public void close() {
        if (asciidoctor != null) {
            try {
                asciidoctor.close();
                logger.info("AsciidocProcessor closed successfully");
            } catch (Exception e) {
                logger.warn("Error closing AsciidocProcessor", e);
            }
        }
    }
}