package com.structurizr.confluence.processor;

import org.asciidoctor.Asciidoctor;
import org.asciidoctor.Options;
import org.asciidoctor.SafeMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Converts AsciiDoc content to HTML using AsciidoctorJ.
 * Handles preprocessing of AsciiDoc syntax before conversion to ADF.
 */
public class AsciiDocConverter {
    
    private static final Logger logger = LoggerFactory.getLogger(AsciiDocConverter.class);
    
    private final Asciidoctor asciidoctor;
    
    public AsciiDocConverter() {
        this.asciidoctor = Asciidoctor.Factory.create();
        logger.info("AsciiDoc converter initialized");
    }
    
    /**
     * Converts AsciiDoc content to HTML.
     * 
     * @param asciiDocContent the AsciiDoc content to convert
     * @param title optional document title
     * @return HTML content
     */
    public String convertToHtml(String asciiDocContent, String title) {
        if (asciiDocContent == null || asciiDocContent.trim().isEmpty()) {
            logger.warn("Empty or null AsciiDoc content provided");
            return "";
        }
        
        try {
            logger.debug("Converting AsciiDoc content to HTML for document: {}", title);
            
            // Configure conversion options
            Options options = Options.builder()
                    .safe(SafeMode.UNSAFE) // Allow all content
                    .backend("html5")
                    .standalone(false) // Only body content, no full HTML document
                    .build();
            
            // Process AsciiDoc content and handle diagram embeds
            String processedContent = preprocessAsciiDocContent(asciiDocContent);
            
            // Convert to HTML
            String htmlContent = asciidoctor.convert(processedContent, options);
            
            logger.debug("Successfully converted AsciiDoc to HTML ({} chars -> {} chars)", 
                    asciiDocContent.length(), htmlContent.length());
            
            return htmlContent;
            
        } catch (Exception e) {
            logger.error("Error converting AsciiDoc to HTML for document: {}", title, e);
            // Fallback: return original content wrapped in HTML
            return "<div class=\"asciidoc-error\">" +
                   "<p><strong>AsciiDoc conversion failed:</strong></p>" +
                   "<pre>" + asciiDocContent + "</pre>" +
                   "</div>";
        }
    }
    
    /**
     * Converts AsciiDoc content to HTML with custom attributes.
     * 
     * @param asciiDocContent the AsciiDoc content to convert
     * @param attributes custom attributes for the conversion
     * @param title optional document title
     * @return HTML content
     */
    public String convertToHtml(String asciiDocContent, Map<String, Object> attributes, String title) {
        if (asciiDocContent == null || asciiDocContent.trim().isEmpty()) {
            logger.warn("Empty or null AsciiDoc content provided");
            return "";
        }
        
        try {
            logger.debug("Converting AsciiDoc content to HTML with custom attributes for document: {}", title);
            
            // Configure conversion options with custom attributes
            Options options = Options.builder()
                    .safe(SafeMode.UNSAFE)
                    .backend("html5")
                    .standalone(false)
                    .attributes(attributes)
                    .build();
            
            // Process AsciiDoc content and handle diagram embeds
            String processedContent = preprocessAsciiDocContent(asciiDocContent);
            
            // Convert to HTML
            String htmlContent = asciidoctor.convert(processedContent, options);
            
            logger.debug("Successfully converted AsciiDoc to HTML with attributes ({} chars -> {} chars)", 
                    asciiDocContent.length(), htmlContent.length());
            
            return htmlContent;
            
        } catch (Exception e) {
            logger.error("Error converting AsciiDoc to HTML with attributes for document: {}", title, e);
            // Fallback: return original content wrapped in HTML
            return "<div class=\"asciidoc-error\">" +
                   "<p><strong>AsciiDoc conversion failed:</strong></p>" +
                   "<pre>" + asciiDocContent + "</pre>" +
                   "</div>";
        }
    }
    
    /**
     * Preprocesses AsciiDoc content to handle special cases like diagram embeds.
     * 
     * @param content the original AsciiDoc content
     * @return processed AsciiDoc content
     */
    private String preprocessAsciiDocContent(String content) {
        // Handle Structurizr diagram embeds: image::embed:diagram_key[]
        // Convert them to simple image references or placeholders
        String processed = content.replaceAll(
            "image::embed:([^\\[]+)\\[\\]",
            "[DIAGRAM: $1]"
        );
        
        // Handle include directives that might reference external files
        processed = processed.replaceAll(
            "include::([^\\[]+)\\[\\]",
            "[INCLUDE: $1]"
        );
        
        // Handle ifndef/ifdef blocks that might cause issues
        processed = processed.replaceAll(
            "ifndef::([^\\[]+)\\[([^\\]]+)\\]",
            ""
        );
        
        logger.trace("Preprocessed AsciiDoc content: {} chars -> {} chars", 
                content.length(), processed.length());
        
        return processed;
    }
    
    
    /**
     * Closes the AsciiDoc converter and releases resources.
     */
    public void close() {
        if (asciidoctor != null) {
            asciidoctor.close();
            logger.info("AsciiDoc converter closed");
        }
    }
}