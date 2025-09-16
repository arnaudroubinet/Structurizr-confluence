package com.structurizr.confluence.processor;

import com.atlassian.adf.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Converts HTML content to Atlassian Document Format (ADF) for Confluence export.
 * Handles conversion of HTML elements to ADF nodes with proper formatting.
 */
public class HtmlToAdfConverter {
    
    private static final Logger logger = LoggerFactory.getLogger(HtmlToAdfConverter.class);
    
    /**
     * Converts HTML content to ADF document.
     * 
     * @param htmlContent the HTML content to convert
     * @param title the document title
     * @return ADF document
     */
    public Document convertToAdf(String htmlContent, String title) {
        logger.info("Converting HTML content to ADF for document: {}", title);
        
        try {
            Document doc = Document.create();
            
            // Add title
            if (title != null && !title.isEmpty()) {
                doc = doc.h1(title);
            }
            
            // Process HTML content and convert to ADF
            doc = processHtmlContent(doc, htmlContent);
            
            logger.info("Successfully converted HTML to ADF document");
            return doc;
            
        } catch (Exception e) {
            logger.error("Error converting HTML to ADF", e);
            // Fallback: create simple text document
            return createFallbackDocument(title, htmlContent);
        }
    }
    
    /**
     * Converts multiple HTML sections to ADF document.
     * 
     * @param sections map of section titles to HTML content
     * @param mainTitle the main document title
     * @return ADF document
     */
    public Document convertSectionsToAdf(Map<String, String> sections, String mainTitle) {
        logger.info("Converting {} sections to ADF document: {}", sections.size(), mainTitle);
        
        try {
            Document doc = Document.create();
            
            // Add main title
            if (mainTitle != null && !mainTitle.isEmpty()) {
                doc = doc.h1(mainTitle);
            }
            
            // Process each section
            for (Map.Entry<String, String> section : sections.entrySet()) {
                String sectionTitle = section.getKey();
                String sectionContent = section.getValue();
                
                // Add section heading
                doc = doc.h2(sectionTitle);
                
                // Process section content
                doc = processHtmlContent(doc, sectionContent);
                
                // Add spacing between sections
                doc = doc.paragraph("");
            }
            
            logger.info("Successfully converted {} sections to ADF document", sections.size());
            return doc;
            
        } catch (Exception e) {
            logger.error("Error converting sections to ADF", e);
            // Fallback: create simple text document
            return createFallbackDocument(mainTitle, sections.toString());
        }
    }
    
    /**
     * Processes HTML content and adds it to the ADF document.
     * 
     * @param doc the ADF document
     * @param htmlContent the HTML content to process
     * @return updated ADF document
     */
    private Document processHtmlContent(Document doc, String htmlContent) {
        if (htmlContent == null || htmlContent.trim().isEmpty()) {
            return doc;
        }
        
        // Clean HTML content
        String cleanedContent = cleanHtml(htmlContent);
        
        // Split content into paragraphs and other elements
        String[] elements = cleanedContent.split("(?=<h[1-6])|(?=<p>)|(?=<ul>)|(?=<ol>)|(?=<pre>)|(?=<blockquote>)");
        
        for (String element : elements) {
            element = element.trim();
            if (element.isEmpty()) continue;
            
            if (element.startsWith("<h1")) {
                doc = addHeading(doc, element, 1);
            } else if (element.startsWith("<h2")) {
                doc = addHeading(doc, element, 2);
            } else if (element.startsWith("<h3")) {
                doc = addHeading(doc, element, 3);
            } else if (element.startsWith("<h4")) {
                doc = addHeading(doc, element, 4);
            } else if (element.startsWith("<h5")) {
                doc = addHeading(doc, element, 5);
            } else if (element.startsWith("<h6")) {
                doc = addHeading(doc, element, 6);
            } else if (element.startsWith("<ul>")) {
                doc = addBulletList(doc, element);
            } else if (element.startsWith("<ol>")) {
                doc = addOrderedList(doc, element);
            } else if (element.startsWith("<pre>")) {
                doc = addCodeBlock(doc, element);
            } else if (element.startsWith("<blockquote>")) {
                doc = addBlockquote(doc, element);
            } else {
                doc = addParagraph(doc, element);
            }
        }
        
        return doc;
    }
    
    /**
     * Adds a heading to the ADF document.
     */
    private Document addHeading(Document doc, String element, int level) {
        String text = extractTextContent(element);
        if (!text.isEmpty()) {
            switch (level) {
                case 1: return doc.h1(text);
                case 2: return doc.h2(text);
                case 3: return doc.h3(text);
                case 4: return doc.h4(text);
                case 5: return doc.h5(text);
                case 6: return doc.h6(text);
                default: return doc.h2(text);
            }
        }
        return doc;
    }
    
    /**
     * Adds a paragraph to the ADF document.
     */
    private Document addParagraph(Document doc, String element) {
        String text = extractTextContent(element);
        if (!text.isEmpty()) {
            return doc.paragraph(text);
        }
        return doc;
    }
    
    /**
     * Adds a bullet list to the ADF document.
     */
    private Document addBulletList(Document doc, String element) {
        String[] items = extractListItems(element);
        if (items.length > 0) {
            // For now, convert to simple paragraphs with bullet points
            for (String item : items) {
                if (!item.trim().isEmpty()) {
                    doc = doc.paragraph("• " + item.trim());
                }
            }
        }
        return doc;
    }
    
    /**
     * Adds an ordered list to the ADF document.
     */
    private Document addOrderedList(Document doc, String element) {
        String[] items = extractListItems(element);
        if (items.length > 0) {
            // For now, convert to simple paragraphs with numbers
            for (int i = 0; i < items.length; i++) {
                if (!items[i].trim().isEmpty()) {
                    doc = doc.paragraph((i + 1) + ". " + items[i].trim());
                }
            }
        }
        return doc;
    }
    
    /**
     * Adds a code block to the ADF document.
     */
    private Document addCodeBlock(Document doc, String element) {
        String code = extractTextContent(element);
        if (!code.isEmpty()) {
            // Use a simple paragraph with monospace indicator
            return doc.paragraph("Code: " + code);
        }
        return doc;
    }
    
    /**
     * Adds a blockquote to the ADF document.
     */
    private Document addBlockquote(Document doc, String element) {
        String text = extractTextContent(element);
        if (!text.isEmpty()) {
            // Use a simple paragraph with quote indicator
            return doc.paragraph("> " + text);
        }
        return doc;
    }
    
    /**
     * Extracts text content from HTML element, removing tags.
     */
    private String extractTextContent(String htmlElement) {
        return htmlElement.replaceAll("<[^>]*>", "").trim();
    }
    
    /**
     * Extracts list items from HTML list element.
     */
    private String[] extractListItems(String listElement) {
        String itemsContent = listElement.replaceAll("</?[uo]l[^>]*>", "");
        return itemsContent.split("</?li[^>]*>");
    }
    
    /**
     * Cleans HTML content by removing unnecessary attributes and normalizing structure.
     */
    private String cleanHtml(String htmlContent) {
        return htmlContent
                .replaceAll("\\s+", " ") // Normalize whitespace
                .replaceAll("<([^>]+)\\s+[^>]*>", "<$1>") // Remove attributes for simplicity
                .trim();
    }
    
    /**
     * Creates a fallback ADF document when conversion fails.
     */
    private Document createFallbackDocument(String title, String content) {
        logger.warn("Creating fallback ADF document for: {}", title);
        
        return Document.create()
                .h1(title != null ? title : "Document")
                .paragraph("Content processing failed. Raw content:")
                .paragraph(content != null ? content : "No content available");
    }
}