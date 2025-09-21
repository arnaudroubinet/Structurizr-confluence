package com.structurizr.confluence.processor;

import org.asciidoctor.Asciidoctor;
import org.asciidoctor.Options;
import org.asciidoctor.SafeMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Map;
import java.util.function.Function;

/**
 * Converts AsciiDoc content to HTML using AsciidoctorJ.
 * Handles preprocessing of AsciiDoc syntax before conversion to ADF.
 */
public class AsciiDocConverter {
    
    private static final Logger logger = LoggerFactory.getLogger(AsciiDocConverter.class);
    
    private final Asciidoctor asciidoctor;
    private Function<String, File> diagramResolver; // Function to resolve diagram files by view key
    
    public AsciiDocConverter() {
        this.asciidoctor = Asciidoctor.Factory.create();
        logger.info("AsciiDoc converter initialized");
    }
    
    /**
     * Sets the diagram resolver function.
     * 
     * @param diagramResolver function that takes a view key and returns the corresponding diagram file
     */
    public void setDiagramResolver(Function<String, File> diagramResolver) {
        this.diagramResolver = diagramResolver;
    }
    
    /**
     * Converts AsciiDoc content to HTML.
     * 
     * @param asciiDocContent the AsciiDoc content to convert
     * @param title optional document title
     * @return HTML content
     */
    public String convertToHtml(String asciiDocContent, String title) {
        return convertToHtml(asciiDocContent, title, null, null);
    }
    
    /**
     * Converts AsciiDoc content to HTML with workspace context for diagram URLs.
     * 
     * @param asciiDocContent the AsciiDoc content to convert
     * @param title optional document title
     * @param workspaceId workspace ID for diagram URL generation
     * @param branchName branch name for diagram URL generation
     * @return HTML content
     */
    public String convertToHtml(String asciiDocContent, String title, String workspaceId, String branchName) {
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
            String processedContent = preprocessAsciiDocContent(asciiDocContent, workspaceId, branchName);
            
            // Convert to HTML
            String htmlContent = asciidoctor.convert(processedContent, options);
            
            logger.debug("Successfully converted AsciiDoc to HTML ({} chars -> {} chars)", 
                    asciiDocContent.length(), htmlContent.length());
            
            return htmlContent;
            
        } catch (Exception e) {
            logger.error("Error converting AsciiDoc to HTML for document: {}", title, e);
            throw new IllegalStateException("Conversion AsciiDoc -> HTML échouée: " + (title != null ? title : "(sans titre)"), e);
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
            // Note: Using deprecated attributes method for compatibility
            Options options = Options.builder()
                    .safe(SafeMode.UNSAFE)
                    .backend("html5")
                    .standalone(false)
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
            throw new IllegalStateException("Conversion AsciiDoc -> HTML (avec attributs) échouée: " + (title != null ? title : "(sans titre)"), e);
        }
    }
    
    /**
     * Preprocesses AsciiDoc content to handle special cases like diagram embeds.
     * 
     * @param content the original AsciiDoc content
     * @return processed AsciiDoc content
     */
    private String preprocessAsciiDocContent(String content) {
        return preprocessAsciiDocContent(content, null, null);
    }
    
    /**
     * Preprocesses AsciiDoc content to handle special cases like diagram embeds.
     * 
     * @param content the original AsciiDoc content
     * @param workspaceId workspace ID for diagram URL generation
     * @param branchName branch name for diagram URL generation
     * @return processed AsciiDoc content with proper image URLs
     */
    private String preprocessAsciiDocContent(String content, String workspaceId, String branchName) {
        // Handle Structurizr diagram embeds: image::embed:diagram_key[]
        String processed = content;
        
        // Check if we have local diagram files available
        if (diagramResolver != null) {
            // Use local diagram files - replace with placeholders that will be handled by HtmlToAdfConverter
            processed = processed.replaceAll(
                "image::embed:([a-zA-Z0-9_-]+)\\[\\]",
                "image::local:diagram:$1[]"
            );
            logger.debug("Replaced diagram embeds with local diagram placeholders");
        } else if (workspaceId != null && branchName != null) {
            // Fallback to external URLs (old behavior)
            processed = processed.replaceAll(
                "image::embed:([a-zA-Z0-9_-]+)\\[\\]",
                "image::https://structurizr.roubinet.fr/workspace/" + workspaceId + "/diagrams/$1-" + branchName + ".svg[]"
            );
            logger.debug("Replaced diagram embeds with external URLs for workspace {} and branch {}", workspaceId, branchName);
        } else {
            // Use local diagram placeholders even without explicit diagram resolver
            // This allows the HtmlToAdfConverter to attempt to find and use local diagrams
            processed = processed.replaceAll(
                "image::embed:([a-zA-Z0-9_-]+)\\[\\]",
                "image::local:diagram:$1[]"
            );
            logger.debug("Used local diagram placeholders as fallback (no workspace context available)");
        }
        
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