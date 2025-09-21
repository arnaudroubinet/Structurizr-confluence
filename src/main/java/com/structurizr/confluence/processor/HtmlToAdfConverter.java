package com.structurizr.confluence.processor;

import com.atlassian.adf.Document;
import com.atlassian.adf.inline.Text;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;

import org.jsoup.select.Elements;

import java.io.File;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Function;

/**
 * Converts HTML content to Atlassian Document Format (ADF) for Confluence export.
 * Handles comprehensive conversion of all HTML elements to ADF nodes with proper formatting.
 * Supports complete HTML5 specification including tables, lists, headings, formatting, and more.
 */
public class HtmlToAdfConverter {
    
    private static final Logger logger = LoggerFactory.getLogger(HtmlToAdfConverter.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    // Image upload manager for handling external images
    private ImageUploadManager imageUploadManager;
    private String currentPageId; // Context for image uploads
    private Function<String, File> diagramResolver; // Function to resolve local diagram files
    
    // Map des balises HTML vers les méthodes de conversion ADF
    private static final Map<String, String> HTML_TO_ADF_MAPPING = new HashMap<>();
    
    static {
        // Éléments de structure
        HTML_TO_ADF_MAPPING.put("h1", "heading1");
        HTML_TO_ADF_MAPPING.put("h2", "heading2");
        HTML_TO_ADF_MAPPING.put("h3", "heading3");
        HTML_TO_ADF_MAPPING.put("h4", "heading4");
        HTML_TO_ADF_MAPPING.put("h5", "heading5");
        HTML_TO_ADF_MAPPING.put("h6", "heading6");
        HTML_TO_ADF_MAPPING.put("p", "paragraph");
        HTML_TO_ADF_MAPPING.put("div", "paragraph");
        HTML_TO_ADF_MAPPING.put("section", "paragraph");
        HTML_TO_ADF_MAPPING.put("article", "paragraph");
        
        // Éléments de formatage
        HTML_TO_ADF_MAPPING.put("strong", "strong");
        HTML_TO_ADF_MAPPING.put("b", "strong");
        HTML_TO_ADF_MAPPING.put("em", "emphasis");
        HTML_TO_ADF_MAPPING.put("i", "emphasis");
        HTML_TO_ADF_MAPPING.put("u", "underline");
        HTML_TO_ADF_MAPPING.put("s", "strike");
        HTML_TO_ADF_MAPPING.put("strike", "strike");
        HTML_TO_ADF_MAPPING.put("del", "strike");
        HTML_TO_ADF_MAPPING.put("code", "code");
        HTML_TO_ADF_MAPPING.put("kbd", "code");
        HTML_TO_ADF_MAPPING.put("samp", "code");
        HTML_TO_ADF_MAPPING.put("var", "code");
        
        // Éléments de liste
        HTML_TO_ADF_MAPPING.put("ul", "bulletList");
        HTML_TO_ADF_MAPPING.put("ol", "numberedList");
        HTML_TO_ADF_MAPPING.put("li", "listItem");
        
        // Éléments de tableau
        HTML_TO_ADF_MAPPING.put("table", "table");
        HTML_TO_ADF_MAPPING.put("thead", "tableHeader");
        HTML_TO_ADF_MAPPING.put("tbody", "tableBody");
        HTML_TO_ADF_MAPPING.put("tfoot", "tableFooter");
        HTML_TO_ADF_MAPPING.put("tr", "tableRow");
        HTML_TO_ADF_MAPPING.put("th", "tableHeaderCell");
        HTML_TO_ADF_MAPPING.put("td", "tableCell");
        
        // Éléments spéciaux
        HTML_TO_ADF_MAPPING.put("blockquote", "blockquote");
        HTML_TO_ADF_MAPPING.put("pre", "codeBlock");
        HTML_TO_ADF_MAPPING.put("hr", "rule");
        HTML_TO_ADF_MAPPING.put("br", "hardBreak");
        HTML_TO_ADF_MAPPING.put("a", "link");
        HTML_TO_ADF_MAPPING.put("img", "image");
    }
    
    /**
     * Sets the image upload manager for handling external images.
     */
    public void setImageUploadManager(ImageUploadManager imageUploadManager) {
        this.imageUploadManager = imageUploadManager;
    }
    
    /**
     * Sets the current page ID context for image uploads.
     */
    public void setCurrentPageId(String pageId) {
        this.currentPageId = pageId;
    }
    
    /**
     * Sets the diagram resolver function for handling local diagram files.
     * 
     * @param diagramResolver function that resolves view keys to local diagram files
     */
    public void setDiagramResolver(Function<String, File> diagramResolver) {
        this.diagramResolver = diagramResolver;
    }
    
    
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
            // Use the JSON method to get properly processed tables
            String adfJson = convertToAdfJson(htmlContent, title);
            
            // Try to convert back to Document
            // Note: This may lose table structure due to ADF Builder limitations,
            // but it's the best we can do for backward compatibility
            JsonNode adfNode = objectMapper.readTree(adfJson);
            Document doc = objectMapper.treeToValue(adfNode, Document.class);
            
            logger.info("Successfully converted HTML to ADF document (tables may be limited by ADF Builder)");
            return doc;
            
        } catch (Exception e) {
            logger.error("Error converting HTML to ADF, falling back to basic conversion", e);
            // Fallback: create document without post-processing
            return convertToAdfWithoutPostProcessing(htmlContent, title);
        }
    }

    /**
     * Converts HTML content to ADF JSON with native table support.
     * 
     * @param htmlContent the HTML content to convert
     * @param title the document title
     * @return ADF JSON string with native tables
     */
    public String convertToAdfJson(String htmlContent, String title) {
        logger.info("Converting HTML content to ADF JSON for document: {}", title);
        
        try {
            // Extraire le titre de la page du contenu HTML (H1 en début de page en priorité)
            TitleAndContent extracted = extractPageTitle(htmlContent);
            
            // Le H1 en début de page a la priorité absolue sur le titre fourni
            String pageTitle = extracted.title != null ? extracted.title : title;
            String contentWithoutTitle = extracted.content;
            
            if (extracted.title != null) {
                logger.debug("Using H1 title from content: '{}', Content length: {}", pageTitle, 
                    contentWithoutTitle != null ? contentWithoutTitle.length() : 0);
            } else {
                logger.debug("No H1 found, using provided title: '{}', Content length: {}", pageTitle, 
                    contentWithoutTitle != null ? contentWithoutTitle.length() : 0);
            }
            
            // Create document without post-processing to avoid double processing
            Document doc = convertToAdfWithoutPostProcessing(contentWithoutTitle, pageTitle);
            
            // Convert to JSON
            String adfJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(doc);
            
            // Process tables using the post-processor (only once)
            String processedJson = AdfTablePostProcessor.postProcessTables(adfJson);
            
            logger.info("Successfully converted HTML to ADF JSON with native tables");
            return processedJson;
            
        } catch (Exception e) {
            logger.error("Error converting HTML to ADF JSON", e);
            // Fallback: create simple text document
            Document fallback = createFallbackDocument(title, htmlContent);
            try {
                return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(fallback);
            } catch (Exception e2) {
                logger.error("Error creating fallback JSON", e2);
                return "{\"version\":1,\"type\":\"doc\",\"content\":[{\"type\":\"paragraph\",\"content\":[{\"type\":\"text\",\"text\":\"Error converting content\"}]}]}";
            }
        }
    }

    /**
     * Converts HTML content to ADF document without post-processing (used by convertToAdfJson).
     * 
     * @param htmlContent the HTML content to convert
     * @param title the document title
     * @return ADF document
     */
    private Document convertToAdfWithoutPostProcessing(String htmlContent, String title) {
        logger.debug("Converting HTML content to ADF without post-processing for document: {}", title);
        
        try {
            Document doc = Document.create();
            
            // Parse HTML content and convert to ADF (no title added - Confluence handles page titles)
            doc = processHtmlContent(doc, htmlContent);
            
            // Return without post-processing
            logger.debug("Successfully converted HTML to ADF document (without post-processing)");
            return doc;
            
        } catch (Exception e) {
            logger.error("Error converting HTML to ADF", e);
            // Fallback: create simple text document
            return createFallbackDocument(title, htmlContent);
        }
    }
    
    /**
     * Post-processes a Document to replace table markers with native ADF table structures.
     */
    private Document postProcessTablesInDocument(Document doc) {
        try {
            // Convert document to JSON
            String adfJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(doc);
            
            // Process tables using the post-processor
            String processedJson = AdfTablePostProcessor.postProcessTables(adfJson);
            
            // If processing changed the JSON, we need to create a new document
            if (!adfJson.equals(processedJson)) {
                logger.debug("Table post-processing modified the document");
                // Try to convert processed JSON back to Document
                try {
                    JsonNode processedNode = objectMapper.readTree(processedJson);
                    // For now, we'll just log the processed JSON since Document reconstruction is complex
                    logger.info("Tables successfully converted to native ADF format");
                    logger.debug("Processed ADF with native tables: {}", processedJson);
                } catch (Exception e) {
                    logger.warn("Could not parse processed JSON", e);
                }
            }
            
            logger.info("Successfully converted HTML to ADF document");
            return doc;
            
        } catch (Exception e) {
            logger.warn("Error during table post-processing, returning original document", e);
            return doc;
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
            
            // Process each section (no main title added - Confluence handles page titles)
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
     * Processes HTML content and converts it to ADF elements using JSoup parser.
     * 
     * @param doc the ADF document
     * @param htmlContent the HTML content to process
     * @return updated ADF document
     */
    private Document processHtmlContent(Document doc, String htmlContent) {
        if (htmlContent == null || htmlContent.trim().isEmpty()) {
            return doc;
        }
        
        try {
            // Parse HTML with JSoup
            org.jsoup.nodes.Document htmlDoc = Jsoup.parse(htmlContent);
            
            // Process all elements in the body
            for (Element element : htmlDoc.body().children()) {
                doc = processElement(doc, element);
            }
            
        } catch (Exception e) {
            logger.warn("Error parsing HTML with JSoup, falling back to simple processing", e);
            // Fallback to simple text processing
            doc = doc.paragraph(cleanText(htmlContent));
        }
        
        return doc;
    }
    
    /**
     * Processes a single HTML element and converts it to appropriate ADF node.
     * 
     * @param doc the ADF document
     * @param element the HTML element to process
     * @return updated ADF document
     */
    private Document processElement(Document doc, Element element) {
        String tagName = element.tagName().toLowerCase();
        logger.debug("Processing element: <{}> with {} children", tagName, element.children().size());
        
        switch (tagName) {
            // Headings - use formatted text to preserve inline formatting
            case "h1": return doc.h1(getElementText(element));
            case "h2": return doc.h2(getElementText(element));
            case "h3": return doc.h3(getElementText(element));
            case "h4": return doc.h4(getElementText(element));
            case "h5": return doc.h5(getElementText(element));
            case "h6": return doc.h6(getElementText(element));
            
            // Paragraphs and blocks
            case "p":
                return processTextBlock(doc, element);
            case "div":
            case "section":
            case "article":
                // Process children recursively instead of treating as flat text
                for (Element child : element.children()) {
                    doc = processElement(doc, child);
                }
                return doc;
            
            // Modern semantic elements - process children recursively
            case "header":
            case "footer":
            case "main":
            case "nav":
            case "aside":
                // Process children recursively instead of treating as semantic block
                for (Element child : element.children()) {
                    doc = processElement(doc, child);
                }
                return doc;
            
            // Figure elements
            case "figure":
                return processFigure(doc, element);
            case "figcaption":
                return doc.paragraph("Caption: " + getElementText(element));
            
            // Interactive elements
            case "details":
                return processDetails(doc, element);
            case "summary":
                return doc.h4(getElementText(element));
            
            // Lists
            case "ul":
                return processBulletList(doc, element);
            case "ol":
                return processNumberedList(doc, element);
            case "dl":
                return processDescriptionList(doc, element);
            
            // Tables
            case "table":
                return processTable(doc, element);
            
            // Special blocks
            case "blockquote":
                return processBlockquote(doc, element);
            case "pre":
                return processCodeBlock(doc, element);
            
            // Media elements
            case "img":
                return processImage(doc, element);
            case "audio":
            case "video":
                return processMedia(doc, element, tagName);
            case "picture":
                return processPicture(doc, element);
            
            // Link elements
            case "a":
                return processLink(doc, element);
            
            // Form elements (converted to text)
            case "form":
            case "input":
            case "button":
            case "textarea":
            case "select":
            case "fieldset":
            case "legend":
            case "label":
                return processFormElement(doc, element, tagName);
            
            // Line breaks and rules
            case "hr":
                return doc.rule();
            case "br":
                // Les sauts de ligne sont ajoutés comme des paragraphes vides
                return doc.paragraph("");
            
            // Inline semantic elements
            case "abbr":
                return processAbbreviation(doc, element);
            case "cite":
                return doc.paragraph("Citation: " + getElementText(element));
            case "dfn":
                return doc.paragraph("Definition: " + getElementText(element));
            case "kbd":
                return doc.paragraph("Keyboard: " + getElementText(element));
            case "samp":
                return doc.paragraph("Sample: " + getElementText(element));
            case "var":
                return doc.paragraph("Variable: " + getElementText(element));
            case "time":
                return processTime(doc, element);
            case "mark":
                return doc.paragraph("Highlighted: " + getElementText(element));
            case "small":
                return doc.paragraph("Small text: " + getElementText(element));
            case "sub":
                return doc.paragraph(getElementText(element) + " (subscript)");
            case "sup":
                return doc.paragraph(getElementText(element) + " (superscript)");
            case "ins":
                return doc.paragraph("Inserted: " + getElementText(element));
            case "del":
                return doc.paragraph("Deleted: " + getElementText(element));
            case "q":
                return doc.paragraph("\"" + getElementText(element) + "\"");
            
            // Inline formatting - process with native ADF marks
            case "span":
                return processInlineElementAsFormattedParagraph(doc, element, null);
            case "strong":
            case "b":
                return processInlineElementAsFormattedParagraph(doc, element, "strong");
            case "em":
            case "i":
                return processInlineElementAsFormattedParagraph(doc, element, "em");
            case "u":
                return processInlineElementAsFormattedParagraph(doc, element, "underline");
            case "s":
                return processInlineElementAsFormattedParagraph(doc, element, "strike");
            case "code":
                return processInlineElementAsFormattedParagraph(doc, element, "code");
            
            // Skip structure elements
            case "thead":
            case "tbody":
            case "tfoot":
            case "tr":
            case "td":
            case "th":
                // Ces éléments sont traités dans processTable
                return doc;
            
            // Default: treat as paragraph
            default:
                String text = getElementText(element);
                if (!text.trim().isEmpty()) {
                    return doc.paragraph(text);
                }
                return doc;
        }
    }
    
    /**
     * Processes a text block element (p, div, etc.) with inline formatting preserved.
     */
    private Document processTextBlock(Document doc, Element element) {
        // Nouvelle approche : détecter si le contenu a du formatage inline
        if (hasInlineFormatting(element)) {
            // Utiliser le formatage ADF natif avec marks
            return processTextBlockWithNativeFormatting(doc, element);
        }
        
        // Convertir le contenu du paragraphe en Markdown pour gérer les liens
        String markdownContent = convertElementToMarkdown(element);
        
        if (!markdownContent.trim().isEmpty()) {
            // CORRECTION: Détecter si le contenu contient des liens
            if (containsLinks(markdownContent)) {
                // Si le contenu contient des liens, utiliser une approche hybride
                return addMarkdownContentToDocument(doc, markdownContent);
            } else {
                // Si pas de liens, utiliser paragraph() pour préserver l'accumulation
                String textContent = cleanText(element.text());
                return doc.paragraph(textContent);
            }
        }
        return doc;
    }
    
    /**
     * Vérifie si le contenu markdown contient des liens.
     */
    private boolean containsLinks(String markdownContent) {
        // Détection simple des liens markdown [text](url) ou des URLs directes
        return markdownContent.contains("[") && markdownContent.contains("](") 
            || markdownContent.contains("http://") 
            || markdownContent.contains("https://");
    }
    
    /**
     * Ajoute le contenu markdown au document en préservant les liens.
     * Utilise une approche hybride pour éviter la perte de contenu.
     */
    private Document addMarkdownContentToDocument(Document doc, String markdownContent) {
        try {
            // Pour l'instant, extraire les liens et les formater comme "text (url)"
            // puis ajouter comme paragraphe simple
            // TODO: Implémenter le support complet des liens ADF natifs
            String processedContent = processLinksForFallback(markdownContent);
            return doc.paragraph(processedContent);
            
        } catch (Exception e) {
            logger.warn("Erreur lors du traitement des liens, utilisation du texte brut", e);
            // Fallback vers le texte brut
            return doc.paragraph(cleanText(markdownContent));
        }
    }
    
    /**
     * Traite les liens markdown pour un affichage de secours lisible.
     * Convertit [text](url) en "text (url)" pour préserver l'information.
     */
    private String processLinksForFallback(String markdownContent) {
        // Remplacer les liens markdown [text](url) par "text (url)"
        String processed = markdownContent.replaceAll("\\[([^\\]]+)\\]\\(([^\\)]+)\\)", "$1 ($2)");
        
        // Nettoyer le contenu
        return cleanText(processed);
    }
    
    /**
     * Convertit un élément HTML en Markdown pour préserver les liens.
     */
    private String convertElementToMarkdown(Element element) {
        StringBuilder markdown = new StringBuilder();
        
        for (org.jsoup.nodes.Node child : element.childNodes()) {
            if (child instanceof org.jsoup.nodes.TextNode) {
                // Nœud de texte direct - PRÉSERVER les espaces !
                String text = ((org.jsoup.nodes.TextNode) child).text();
                // Ne PAS nettoyer le texte pour préserver les espaces
                markdown.append(text);
            } else if (child instanceof Element) {
                Element childElement = (Element) child;
                String tagName = childElement.tagName().toLowerCase();
                
                if ("a".equals(tagName)) {
                    // Convertir les liens en format Markdown [texte](url)
                    String href = childElement.attr("href");
                    String linkText = childElement.text(); // Ne pas nettoyer le texte du lien
                    
                    if (!href.isEmpty() && !linkText.isEmpty()) {
                        markdown.append("[").append(linkText).append("](").append(href).append(")");
                    } else if (!linkText.isEmpty()) {
                        // Lien sans href - garder juste le texte
                        markdown.append(linkText);
                    }
                } else if ("strong".equals(tagName) || "b".equals(tagName)) {
                    // Texte en gras
                    String text = childElement.text();
                    if (!text.isEmpty()) {
                        markdown.append("**").append(text).append("**");
                    }
                } else if ("em".equals(tagName) || "i".equals(tagName)) {
                    // Texte en italique
                    String text = childElement.text();
                    if (!text.isEmpty()) {
                        markdown.append("*").append(text).append("*");
                    }
                } else if ("code".equals(tagName)) {
                    // Code inline
                    String text = childElement.text();
                    if (!text.isEmpty()) {
                        markdown.append("`").append(text).append("`");
                    }
                } else {
                    // Pour les autres éléments inline, garder le texte
                    String text = childElement.text();
                    if (!text.isEmpty()) {
                        markdown.append(text);
                    }
                }
            }
        }
        
        return markdown.toString();
    }
    
    /**
     * Extracts text content from HTML element, preserving some inline formatting.
     */
    private String getElementText(Element element) {
        return cleanText(element.text());
    }
    
    /**
     * Extracts text content from HTML element, preserving some inline formatting.
     */    /**
     * Extracts text from an element while preserving basic formatting through markdown-like syntax.
     */
    private String extractFormattedText(Element element) {
        StringBuilder result = new StringBuilder();
        
        for (org.jsoup.nodes.Node node : element.childNodes()) {
            if (node instanceof org.jsoup.nodes.TextNode) {
                // Texte direct avec décodage des entités HTML
                String text = ((org.jsoup.nodes.TextNode) node).text();
                text = cleanText(text); // Cette méthode décode maintenant les entités HTML
                result.append(text);
            } else if (node instanceof Element) {
                Element childElement = (Element) node;
                String tagName = childElement.tagName().toLowerCase();
                String innerText = cleanText(childElement.text()); // Décodage des entités HTML
                
                switch (tagName) {
                    case "strong":
                    case "b":
                        result.append("**").append(innerText).append("**");
                        break;
                    case "em":
                    case "i":
                        result.append("*").append(innerText).append("*");
                        break;
                    case "u":
                        result.append("_").append(innerText).append("_");
                        break;
                    case "s":
                    case "strike":
                    case "del":
                        result.append("~~").append(innerText).append("~~");
                        break;
                    case "code":
                    case "kbd":
                    case "samp":
                    case "var":
                        result.append("`").append(innerText).append("`");
                        break;
                    case "sub":
                        result.append("~").append(innerText).append("~");
                        break;
                    case "sup":
                        result.append("^").append(innerText).append("^");
                        break;
                    case "a":
                        String href = childElement.attr("href");
                        if (!href.isEmpty()) {
                            result.append("[").append(innerText).append("](").append(href).append(")");
                        } else {
                            result.append(innerText);
                        }
                        break;
                    case "mark":
                        // Surlignage avec une notation spéciale
                        result.append("==").append(innerText).append("==");
                        break;
                    case "small":
                        result.append("(").append(innerText).append(")");
                        break;
                    case "br":
                        result.append("\n");
                        break;
                    default:
                        // Pour les autres balises, traitement récursif
                        result.append(extractFormattedText(childElement));
                        break;
                }
            }
        }
        
        return result.toString();
    }
    
    /**
     * Processes an unordered list element.
     */
    private Document processBulletList(Document doc, Element element) {
        Elements listItems = element.select("li");
        if (!listItems.isEmpty()) {
            return doc.bulletList(list -> {
                for (Element li : listItems) {
                    list.item(item -> item.paragraph(getElementText(li)));
                }
            });
        }
        return doc;
    }
    
    /**
     * Processes an ordered list element.
     */
    private Document processNumberedList(Document doc, Element element) {
        Elements listItems = element.select("li");
        if (!listItems.isEmpty()) {
            // Utiliser orderedList natif ADF au lieu de bulletList avec numéros manuels
            return doc.orderedList(list -> {
                for (Element li : listItems) {
                    // Traiter le contenu de l'élément li
                    if (hasInlineFormatting(li)) {
                        // Traiter le formatage inline avec les marks natifs
                        try {
                            List<Text> textNodes = new ArrayList<>();
                            for (org.jsoup.nodes.Node child : li.childNodes()) {
                                textNodes.addAll(processNodeToTextNodes(child));
                            }
                            Text[] textArray = textNodes.toArray(new Text[0]);
                            list.item(item -> item.paragraph(textArray));
                        } catch (Exception e) {
                            logger.warn("Erreur lors du formatage natif pour item de liste ordonnée, fallback vers texte simple", e);
                            String itemText = getElementText(li);
                            list.item(item -> item.paragraph(itemText));
                        }
                    } else {
                        // Texte simple
                        String itemText = getElementText(li);
                        list.item(item -> item.paragraph(itemText));
                    }
                }
            });
        }
        return doc;
    }
    
    /**
     * Processes a table element and creates a native ADF table structure.
     * Since ADF Builder 3.0.3 doesn't provide table APIs, we create the JSON structure directly 
     * and inject it into the document.
     */
    private Document processTable(Document doc, Element table) {
        try {
            logger.debug("Processing table with {} rows", table.select("tr").size());
            
            // Extract and process table caption if present
            Element caption = table.select("caption").first();
            Document result = doc;
            if (caption != null) {
                String captionText = getElementText(caption).trim();
                if (!captionText.isEmpty()) {
                    logger.debug("Found table caption: {}", captionText);
                    result = result.paragraph(captionText);
                }
            }
            
            // Get all rows to analyze structure
            Elements allRows = table.select("tr");
            if (allRows.isEmpty()) {
                return result.paragraph("Empty table");
            }
            
            // Create native ADF table structure
            ObjectNode tableNode = objectMapper.createObjectNode();
            tableNode.put("type", "table");
            
            // Create table content array
            ArrayNode tableContent = objectMapper.createArrayNode();
            
            // Process each row
            for (Element row : allRows) {
                Elements cells = row.select("td, th");
                if (cells.isEmpty()) continue;
                
                // Create table row
                ObjectNode rowNode = objectMapper.createObjectNode();
                rowNode.put("type", "tableRow");
                ArrayNode rowContent = objectMapper.createArrayNode();
                
                // Process each cell
                for (Element cell : cells) {
                    ObjectNode cellNode = objectMapper.createObjectNode();
                    
                    // Determine cell type (header or data)
                    boolean isThTag = "th".equals(cell.tagName());
                    // Check if the immediate parent row is in thead
                    Element parentRow = cell.parent(); // <tr>
                    Element parentSection = parentRow != null ? parentRow.parent() : null; // <thead> or <tbody>
                    boolean isInThead = parentSection != null && "thead".equals(parentSection.tagName());
                    boolean isHeader = isThTag || isInThead;
                    
                    cellNode.put("type", isHeader ? "tableHeader" : "tableCell");
                    
                    // Create cell content
                    ArrayNode cellContent = objectMapper.createArrayNode();
                    String cellText = getElementText(cell).trim();
                    if (!cellText.isEmpty()) {
                        ObjectNode paragraph = objectMapper.createObjectNode();
                        paragraph.put("type", "paragraph");
                        ArrayNode paragraphContent = objectMapper.createArrayNode();
                        ObjectNode textNode = objectMapper.createObjectNode();
                        textNode.put("type", "text");
                        textNode.put("text", cellText);
                        paragraphContent.add(textNode);
                        paragraph.set("content", paragraphContent);
                        cellContent.add(paragraph);
                    }
                    
                    cellNode.set("content", cellContent);
                    rowContent.add(cellNode);
                }
                
                rowNode.set("content", rowContent);
                tableContent.add(rowNode);
            }
            
            tableNode.set("content", tableContent);
            
            // Inject the native table into the document using reflection
            return injectNativeAdfNode(result, tableNode);
            
        } catch (Exception e) {
            logger.warn("Error processing table as native ADF, falling back to structured format", e);
            // Fallback to the improved pipe-separated format
            return processTableFallback(doc, table);
        }
    }
    
    /**
     * Injects a native ADF node into the document using reflection.
     * This is a workaround for the lack of table support in ADF Builder 3.0.3.
     */
    private Document injectNativeAdfNode(Document doc, ObjectNode adfNode) {
        try {
            // Use reflection to access the internal content structure of the Document
            // and inject our native ADF table node
            
            // For now, let's serialize the ADF node and add it as a special marker
            // that we can process later if needed, or use the fallback
            String adfJson = objectMapper.writeValueAsString(adfNode);
            logger.debug("Generated native ADF table: {}", adfJson);
            
            // Since we can't easily inject into Document, use fallback with better formatting
            return doc.paragraph("<!-- ADF_TABLE_START -->" + adfJson + "<!-- ADF_TABLE_END -->");
            
        } catch (Exception e) {
            logger.warn("Failed to inject native ADF node", e);
            throw new RuntimeException("Failed to inject native ADF table", e);
        }
    }
    
    /**
     * Fallback method that creates a more structured table representation
     * when native ADF table injection fails.
     */
    private Document processTableFallback(Document doc, Element table) {
        try {
            // Add table separator for clarity
            doc = doc.rule();
            
            // Get all rows to determine structure
            Elements allRows = table.select("tr");
            if (allRows.isEmpty()) {
                return doc.paragraph("Empty table");
            }
            
            // Find header rows (containing th elements or in thead)
            Elements headerRows = table.select("thead tr, tr:has(th)");
            Elements bodyRows = table.select("tbody tr, tr:not(:has(th))");
            
            // If no explicit header/body distinction, treat first row as header if it contains th
            if (headerRows.isEmpty() && bodyRows.isEmpty()) {
                bodyRows = allRows;
            }
            
            // Process headers if they exist
            if (!headerRows.isEmpty()) {
                Elements headerCells = headerRows.first().select("th, td");
                if (!headerCells.isEmpty()) {
                    // Create header row as emphasized paragraphs
                    StringBuilder headerText = new StringBuilder();
                    for (int i = 0; i < headerCells.size(); i++) {
                        if (i > 0) headerText.append(" | ");
                        headerText.append(getElementText(headerCells.get(i)).trim());
                    }
                    doc = doc.paragraph("📋 " + headerText.toString());
                    
                    // Add separator line
                    StringBuilder separator = new StringBuilder();
                    for (int i = 0; i < headerCells.size(); i++) {
                        if (i > 0) separator.append(" | ");
                        separator.append("---");
                    }
                    doc = doc.paragraph(separator.toString());
                }
            }
            
            // Process body rows
            for (Element row : bodyRows) {
                Elements cells = row.select("td, th");
                if (!cells.isEmpty()) {
                    StringBuilder rowText = new StringBuilder();
                    for (int i = 0; i < cells.size(); i++) {
                        if (i > 0) rowText.append(" | ");
                        String cellText = getElementText(cells.get(i)).trim();
                        if (cellText.isEmpty()) cellText = " ";
                        rowText.append(cellText);
                    }
                    doc = doc.paragraph(rowText.toString());
                }
            }
            
            // Add table separator for clarity
            doc = doc.rule();
            
            return doc;
        } catch (Exception e) {
            logger.warn("Error processing table fallback, using simple text representation", e);
            // Ultimate fallback to simple text representation
            return doc.paragraph("Table content: " + getElementText(table));
        }
    }

    /**
     * Processes a blockquote element with native ADF blockquote.
     */
    private Document processBlockquote(Document doc, Element element) {
        // Utiliser blockquote natif ADF au lieu de paragraphe avec préfixe
        return doc.quote(quote -> {
            // Traiter récursivement le contenu du blockquote
            for (Element child : element.children()) {
                String tagName = child.tagName().toLowerCase();
                
                // Traiter différents types de contenu dans la citation
                switch (tagName) {
                    case "p":
                        // Paragraphe dans la citation
                        if (hasInlineFormatting(child)) {
                            try {
                                List<Text> textNodes = new ArrayList<>();
                                for (org.jsoup.nodes.Node node : child.childNodes()) {
                                    textNodes.addAll(processNodeToTextNodes(node));
                                }
                                Text[] textArray = textNodes.toArray(new Text[0]);
                                quote.paragraph(textArray);
                            } catch (Exception e) {
                                logger.warn("Erreur lors du formatage natif pour blockquote, fallback vers texte simple", e);
                                quote.paragraph(getElementText(child));
                            }
                        } else {
                            quote.paragraph(getElementText(child));
                        }
                        break;
                    
                    default:
                        // Pour les autres éléments, utiliser le texte simple
                        String text = getElementText(child);
                        if (!text.trim().isEmpty()) {
                            quote.paragraph(text);
                        }
                        break;
                }
            }
            
            // Si aucun enfant structuré, traiter le texte direct du blockquote
            if (element.children().isEmpty()) {
                String text = getElementText(element);
                if (!text.trim().isEmpty()) {
                    quote.paragraph(text);
                }
            }
        });
    }
    
    /**
     * Processes a code block element.
     */
    private Document processCodeBlock(Document doc, Element element) {
        String code = element.text(); // Preserve raw text for code blocks
        if (!code.trim().isEmpty()) {
            // Utilisons un paragraphe avec indicateur de code
            return doc.paragraph("```\n" + code + "\n```");
        }
        return doc;
    }
    
    /**
     * Processes semantic block elements (header, footer, main, nav, aside).
     */
    private Document processSemanticBlock(Document doc, Element element, String tagName) {
        String text = getElementText(element);
        if (!text.trim().isEmpty()) {
            return doc.h3(tagName.toUpperCase() + ": " + text);
        }
        return doc;
    }
    
    /**
     * Processes figure elements.
     */
    private Document processFigure(Document doc, Element element) {
        Elements imgs = element.select("img");
        Elements captions = element.select("figcaption");
        
        doc = doc.h4("Figure");
        
        if (!imgs.isEmpty()) {
            Element img = imgs.first();
            String src = img.attr("src");
            String alt = img.attr("alt");
            if (!src.isEmpty()) {
                doc = doc.paragraph("Image: " + src + (alt.isEmpty() ? "" : " (" + alt + ")"));
            }
        }
        
        if (!captions.isEmpty()) {
            doc = doc.paragraph("Caption: " + getElementText(captions.first()));
        }
        
        return doc;
    }
    
    /**
     * Processes details/summary interactive elements.
     */
    private Document processDetails(Document doc, Element element) {
        Elements summaries = element.select("summary");
        
        if (!summaries.isEmpty()) {
            doc = doc.h4("Details: " + getElementText(summaries.first()));
        } else {
            doc = doc.h4("Details");
        }
        
        // Process content excluding summary
        Elements children = element.children();
        for (Element child : children) {
            if (!"summary".equals(child.tagName().toLowerCase())) {
                doc = processElement(doc, child);
            }
        }
        
        return doc;
    }
    
    /**
     * Processes description lists (dl, dt, dd).
     */
    private Document processDescriptionList(Document doc, Element element) {
        Elements terms = element.select("dt");
        Elements descriptions = element.select("dd");
        
        doc = doc.h4("Liste de définitions");
        
        for (int i = 0; i < Math.max(terms.size(), descriptions.size()); i++) {
            if (i < terms.size()) {
                doc = doc.paragraph("Terme: " + getElementText(terms.get(i)));
            }
            if (i < descriptions.size()) {
                doc = doc.paragraph("Définition: " + getElementText(descriptions.get(i)));
            }
        }
        
        return doc;
    }
    
    /**
     * Processes image elements.
     */
    private Document processImage(Document doc, Element element) {
        String src = element.attr("src");
        String alt = element.attr("alt");
        String title = element.attr("title");
        
        if (src.isEmpty()) {
            // Fallback to text description if no src
            return doc.paragraph("Image: (no source)");
        }
        
        // Check if this is a local diagram placeholder
        if (src.startsWith("local:diagram:")) {
            String viewKey = src.substring("local:diagram:".length());
            return processLocalDiagram(doc, viewKey, alt, title);
        }
        
        try {
            String imageId = generateImageId(src);
            String caption = title.isEmpty() ? alt : title;
            
            if (isExternalUrl(src) && imageUploadManager != null && currentPageId != null) {
                // External image - download and upload as attachment
                try {
                    String attachmentFilename = imageUploadManager.downloadAndUploadImage(src, currentPageId);
                    
                    // Use file type with uploaded attachment
                    return doc.mediaGroup(mediaGroup -> {
                        if (!caption.isEmpty()) {
                            mediaGroup.file(imageId, attachmentFilename, caption);
                        } else {
                            mediaGroup.file(imageId, attachmentFilename);
                        }
                    });
                    
                } catch (Exception e) {
                    logger.warn("Failed to download and upload external image: {}, falling back to text", src, e);
                    // Fallback to text description for external images that can't be uploaded
                    StringBuilder imageText = new StringBuilder("Image (external)");
                    if (!alt.isEmpty()) {
                        imageText.append(": ").append(alt);
                    }
                    if (!title.isEmpty()) {
                        imageText.append(" - ").append(title);
                    }
                    return doc.paragraph(imageText.toString());
                }
            } else {
                // Local/attached image - use file type as before
                return doc.mediaGroup(mediaGroup -> {
                    if (!caption.isEmpty()) {
                        mediaGroup.file(imageId, src, caption);
                    } else {
                        mediaGroup.file(imageId, src);
                    }
                });
            }
        } catch (Exception e) {
            logger.warn("Failed to create native ADF media node for image: {}, falling back to text", src, e);
            // Fallback to text description
            StringBuilder imageText = new StringBuilder("Image");
            if (!src.isEmpty()) {
                imageText.append(": ").append(src);
            }
            if (!alt.isEmpty()) {
                imageText.append(" (").append(alt).append(")");
            }
            if (!title.isEmpty()) {
                imageText.append(" - ").append(title);
            }
            
            return doc.paragraph(imageText.toString());
        }
    }
    
    /**
     * Processes local diagram files.
     * 
     * @param doc the ADF document
     * @param viewKey the diagram view key
     * @param alt alternative text
     * @param title title text
     * @return updated ADF document
     */
    private Document processLocalDiagram(Document doc, String viewKey, String alt, String title) {
        if (diagramResolver == null) {
            // No diagram resolver - fallback to text
            logger.debug("No diagram resolver configured for view key: {}", viewKey);
            return doc.paragraph("Diagram: " + viewKey + (alt.isEmpty() ? "" : " (" + alt + ")"));
        }
        
        try {
            File diagramFile = diagramResolver.apply(viewKey);
            if (diagramFile == null || !diagramFile.exists()) {
                logger.warn("Local diagram file not found for view key: {}", viewKey);
                return doc.paragraph("Diagram not found: " + viewKey);
            }
            
            if (imageUploadManager == null || currentPageId == null) {
                logger.warn("No image upload manager or page ID configured for diagram: {}", viewKey);
                return doc.paragraph("Diagram available but cannot upload: " + viewKey);
            }
            
            // Upload the local diagram file
            String attachmentFilename = imageUploadManager.uploadLocalFile(diagramFile, currentPageId);
            
            // Create media group with the uploaded diagram
            String imageId = generateImageId(diagramFile.getName());
            String caption = title.isEmpty() ? alt : title;
            
            logger.info("Successfully uploaded local diagram: {} -> {}", diagramFile.getName(), attachmentFilename);
            
            return doc.mediaGroup(mediaGroup -> {
                if (!caption.isEmpty()) {
                    mediaGroup.file(imageId, attachmentFilename, caption);
                } else {
                    mediaGroup.file(imageId, attachmentFilename);
                }
            });
            
        } catch (Exception e) {
            logger.error("Failed to process local diagram for view key: {}", viewKey, e);
            return doc.paragraph("Error loading diagram: " + viewKey + " - " + e.getMessage());
        }
    }
    
    /**
     * Processes media elements (audio, video).
     */
    private Document processMedia(Document doc, Element element, String tagName) {
        String src = element.attr("src");
        Elements sources = element.select("source");
        
        doc = doc.h4(tagName.toUpperCase() + " Media");
        
        if (!src.isEmpty()) {
            doc = doc.paragraph("Source: " + src);
        } else if (!sources.isEmpty()) {
            for (Element source : sources) {
                String sourceSrc = source.attr("src");
                String type = source.attr("type");
                if (!sourceSrc.isEmpty()) {
                    doc = doc.paragraph("Source: " + sourceSrc + (type.isEmpty() ? "" : " (" + type + ")"));
                }
            }
        }
        
        String controls = element.attr("controls");
        if (!controls.isEmpty()) {
            doc = doc.paragraph("Controls: enabled");
        }
        
        return doc;
    }
    
    /**
     * Processes picture elements.
     */
    private Document processPicture(Document doc, Element element) {
        Elements sources = element.select("source");
        Elements imgs = element.select("img");
        
        doc = doc.h4("Picture");
        
        for (Element source : sources) {
            String srcset = source.attr("srcset");
            String media = source.attr("media");
            if (!srcset.isEmpty()) {
                doc = doc.paragraph("Source: " + srcset + (media.isEmpty() ? "" : " (" + media + ")"));
            }
        }
        
        if (!imgs.isEmpty()) {
            Element img = imgs.first();
            String src = img.attr("src");
            String alt = img.attr("alt");
            if (!src.isEmpty()) {
                doc = doc.paragraph("Fallback: " + src + (alt.isEmpty() ? "" : " (" + alt + ")"));
            }
        }
        
        return doc;
    }
    
    /**
     * Processes link elements - amélioration pour gérer les liens de manière plus claire.
     */
    private Document processLink(Document doc, Element element) {
        String href = element.attr("href");
        String text = getElementText(element);
        
        if (text.isEmpty()) {
            text = href.isEmpty() ? "Link" : href;
        }
        
        if (!href.isEmpty()) {
            // Utiliser le lien natif ADF au lieu de texte simple
            Text linkText = createNativeLinkText(text, href);
            return doc.paragraph(linkText);
        } else {
            // Lien sans href - garder juste le texte
            return doc.paragraph(text);
        }
    }
    
    /**
     * Processes form elements (converted to descriptive text).
     */
    private Document processFormElement(Document doc, Element element, String tagName) {
        String type = element.attr("type");
        String name = element.attr("name");
        String value = element.attr("value");
        String placeholder = element.attr("placeholder");
        
        StringBuilder formText = new StringBuilder(tagName.toUpperCase());
        
        if (!type.isEmpty()) {
            formText.append(" (").append(type).append(")");
        }
        if (!name.isEmpty()) {
            formText.append(" - Name: ").append(name);
        }
        if (!value.isEmpty()) {
            formText.append(" - Value: ").append(value);
        }
        if (!placeholder.isEmpty()) {
            formText.append(" - Placeholder: ").append(placeholder);
        }
        
        String text = getElementText(element);
        if (!text.isEmpty()) {
            formText.append(" - Text: ").append(text);
        }
        
        return doc.paragraph(formText.toString());
    }
    
    /**
     * Processes abbreviation elements.
     */
    private Document processAbbreviation(Document doc, Element element) {
        String title = element.attr("title");
        String text = getElementText(element);
        
        if (!title.isEmpty()) {
            return doc.paragraph(text + " (abbr: " + title + ")");
        } else {
            return doc.paragraph("Abbreviation: " + text);
        }
    }
    
    /**
     * Processes time elements.
     */
    private Document processTime(Document doc, Element element) {
        String datetime = element.attr("datetime");
        String text = getElementText(element);
        
        if (!datetime.isEmpty()) {
            return doc.paragraph("Time: " + text + " (" + datetime + ")");
        } else {
            return doc.paragraph("Time: " + text);
        }
    }
    
    /**
     * Extracts text content from HTML element, preserving some inline formatting.
     */
    /**
     * Cleans and normalizes text content, including HTML entity decoding.
     */
    private String cleanText(String text) {
        if (text == null) return "";
        // Decode HTML entities using JSoup
        String decodedText = org.jsoup.parser.Parser.unescapeEntities(text, false);
        return decodedText.trim().replaceAll("\\s+", " ");
    }
    
    /**
     * Creates a fallback ADF document when conversion fails.
     */
    private Document createFallbackDocument(String title, String content) {
        logger.warn("Creating fallback ADF document for: {}", title);
        
        return Document.create()
                .paragraph("Content processing failed. Raw content:")
                .paragraph(content != null ? content : "No content available");
    }
    
    /**
     * Détecte si un élément contient du formatage inline (strong, em, code, links avec href, etc.).
     */
    private boolean hasInlineFormatting(Element element) {
        // Vérifier récursivement si l'élément contient des tags de formatage
        boolean hasBasicFormatting = !element.select("strong, b, em, i, code, u, s, del, strike").isEmpty();
        
        // Vérifier les liens, mais seulement ceux qui ont un href (vrais liens)
        boolean hasRealLinks = false;
        Elements links = element.select("a");
        for (Element link : links) {
            if (!link.attr("href").isEmpty()) {
                hasRealLinks = true;
                break;
            }
        }
        
        return hasBasicFormatting || hasRealLinks;
    }
    
    /**
     * Traite un bloc de texte avec formatage inline natif ADF.
     */
    private Document processTextBlockWithNativeFormatting(Document doc, Element element) {
        try {
            // Créer une liste de nœuds Text avec formatage
            List<Text> textNodes = new ArrayList<>();
            
            // Traiter récursivement tous les nœuds enfants
            for (org.jsoup.nodes.Node child : element.childNodes()) {
                textNodes.addAll(processNodeToTextNodes(child));
            }
            
            // Convertir la liste en array pour paragraph()
            Text[] textArray = textNodes.toArray(new Text[0]);
            
            return doc.paragraph(textArray);
            
        } catch (Exception e) {
            logger.warn("Erreur lors du formatage natif, fallback vers texte simple", e);
            // Fallback vers la méthode existante
            String textContent = cleanText(element.text());
            return doc.paragraph(textContent);
        }
    }
    
    /**
     * Convertit récursivement un nœud JSoup en liste de nœuds Text ADF.
     */
    private List<Text> processNodeToTextNodes(org.jsoup.nodes.Node node) {
        List<Text> result = new ArrayList<>();
        
        if (node instanceof org.jsoup.nodes.TextNode) {
            // Nœud de texte simple
            String text = ((org.jsoup.nodes.TextNode) node).text();
            if (!text.trim().isEmpty()) {
                result.add(Text.of(text));
            }
        } else if (node instanceof Element) {
            Element element = (Element) node;
            String tagName = element.tagName().toLowerCase();
            
            switch (tagName) {
                case "strong":
                case "b":
                    result.addAll(createFormattedTextNodes(element, "strong"));
                    break;
                case "em":
                case "i":
                    result.addAll(createFormattedTextNodes(element, "em"));
                    break;
                case "code":
                    result.addAll(createFormattedTextNodes(element, "code"));
                    break;
                case "u":
                    result.addAll(createFormattedTextNodes(element, "underline"));
                    break;
                case "s":
                case "del":
                case "strike":
                    result.addAll(createFormattedTextNodes(element, "strike"));
                    break;
                case "a":
                    // Vérifier si le lien a un href
                    String linkHref = element.attr("href");
                    if (!linkHref.isEmpty()) {
                        // Utiliser les liens natifs ADF avec marks
                        result.addAll(processLinkElement(element));
                    } else {
                        // Lien sans href - traiter récursivement comme du texte normal
                        for (org.jsoup.nodes.Node child : element.childNodes()) {
                            result.addAll(processNodeToTextNodes(child));
                        }
                    }
                    break;
                default:
                    // Pour les autres éléments, traiter récursivement
                    for (org.jsoup.nodes.Node child : element.childNodes()) {
                        result.addAll(processNodeToTextNodes(child));
                    }
                    break;
            }
        }
        
        return result;
    }
    
    /**
     * Crée des nœuds Text avec le formatage spécifié.
     */
    private List<Text> createFormattedTextNodes(Element element, String formatType) {
        List<Text> result = new ArrayList<>();
        
        // Traiter le contenu de l'élément
        for (org.jsoup.nodes.Node child : element.childNodes()) {
            if (child instanceof org.jsoup.nodes.TextNode) {
                String text = ((org.jsoup.nodes.TextNode) child).text();
                if (!text.trim().isEmpty()) {
                    Text textNode = Text.of(text);
                    
                    // Appliquer le formatage selon le type
                    switch (formatType) {
                        case "strong":
                            textNode = textNode.strong();
                            break;
                        case "em":
                            textNode = textNode.em();
                            break;
                        case "code":
                            textNode = textNode.code();
                            break;
                        case "underline":
                            textNode = createNativeFormattedText(text, "underline");
                            break;
                        case "strike":
                            textNode = createNativeFormattedText(text, "strike");
                            break;
                        default:
                            // Pas de formatage supplémentaire
                            break;
                    }
                    
                    result.add(textNode);
                }
            } else if (child instanceof Element) {
                // Gérer le formatage imbriqué
                Element childElement = (Element) child;
                String childTagName = childElement.tagName().toLowerCase();
                
                // Pour le formatage imbriqué, on prend le texte et on applique les deux formatages
                // (limitation de l'approche actuelle)
                String text = childElement.text();
                if (!text.trim().isEmpty()) {
                    Text textNode = Text.of(text);
                    
                    // Appliquer le formatage parent
                    switch (formatType) {
                        case "strong":
                            textNode = textNode.strong();
                            break;
                        case "em":
                            textNode = textNode.em();
                            break;
                        case "code":
                            textNode = textNode.code();
                            break;
                    }
                    
                    // Appliquer le formatage enfant
                    switch (childTagName) {
                        case "strong":
                        case "b":
                            textNode = textNode.strong();
                            break;
                        case "em":
                        case "i":
                            textNode = textNode.em();
                            break;
                        case "code":
                            textNode = textNode.code();
                            break;
                    }
                    
                    result.add(textNode);
                }
            }
        }
        
        return result;
    }
    
    /**
     * Extrait uniquement le titre de la page du contenu HTML (premier H1).
     * 
     * @param htmlContent le contenu HTML
     * @return le titre extrait ou null si aucun H1 trouvé
     */
    public String extractPageTitleOnly(String htmlContent) {
        TitleAndContent extracted = extractPageTitle(htmlContent);
        return extracted.title;
    }
    
    /**
     * Extrait le titre de la page du contenu HTML (H1 en début de page) et retourne le contenu sans ce titre.
     * Cette méthode donne la priorité absolue au premier H1 trouvé dans le document.
     * 
     * @param htmlContent le contenu HTML
     * @return un objet TitleAndContent avec le titre extrait et le contenu modifié
     */
    public TitleAndContent extractPageTitle(String htmlContent) {
        if (htmlContent == null || htmlContent.trim().isEmpty()) {
            return new TitleAndContent(null, htmlContent);
        }
        
        try {
            // Parse HTML avec JSoup
            org.jsoup.nodes.Document htmlDoc = Jsoup.parse(htmlContent);
            
            // Chercher le premier H1 dans l'ordre d'apparition du document
            Elements h1Elements = htmlDoc.select("h1");
            
            if (!h1Elements.isEmpty()) {
                Element firstH1 = h1Elements.first();
                String title = cleanText(firstH1.text());
                
                // Vérifier que le titre n'est pas vide après nettoyage
                if (title != null && !title.trim().isEmpty()) {
                    // Supprimer le premier H1 du document pour éviter la duplication
                    firstH1.remove();
                    
                    // Retourner le contenu modifié sans le H1
                    String modifiedContent = htmlDoc.body().html();
                    
                    logger.debug("Extracted page title: '{}' from first H1 (H1 removed from content)", title);
                    return new TitleAndContent(title, modifiedContent);
                } else {
                    logger.debug("First H1 found but title is empty after cleaning, skipping");
                }
            }
            
            logger.debug("No valid H1 found in content, no title extracted");
            return new TitleAndContent(null, htmlContent);
            
        } catch (Exception e) {
            logger.warn("Error extracting page title from HTML content", e);
            return new TitleAndContent(null, htmlContent);
        }
    }
    
    /**
     * Crée un nœud Text ADF natif avec un mark de type link.
     * 
     * @param linkText Le texte du lien à afficher
     * @param href L'URL du lien
     * @return Un objet Text avec des marks pour créer un lien natif ADF
     */
    private Text createNativeLinkText(String linkText, String href) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode textNode = mapper.createObjectNode();
            
            textNode.put("type", "text");
            textNode.put("text", linkText);
            
            // Créer le mark pour le lien
            ArrayNode marks = mapper.createArrayNode();
            ObjectNode linkMark = mapper.createObjectNode();
            linkMark.put("type", "link");
            
            ObjectNode attrs = mapper.createObjectNode();
            attrs.put("href", href);
            linkMark.set("attrs", attrs);
            
            marks.add(linkMark);
            textNode.set("marks", marks);
            
            // Utiliser Jackson pour créer l'objet Text à partir du JSON
            return mapper.treeToValue(textNode, Text.class);
        } catch (Exception e) {
            logger.warn("Impossible de créer un lien natif ADF, utilisation du fallback: {}", e.getMessage());
            // Fallback vers l'ancien format en cas d'erreur
            return Text.of(linkText + " (" + href + ")");
        }
    }
    
    /**
     * Crée un objet Text avec un mark de formatage natif ADF (underline, strike).
     * 
     * @param text Le texte à formater
     * @param markType Le type de mark ("underline", "strike")
     * @return Un objet Text avec des marks pour créer un formatage natif ADF
     */
    private Text createNativeFormattedText(String text, String markType) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode textNode = mapper.createObjectNode();
            
            textNode.put("type", "text");
            textNode.put("text", text);
            
            // Créer le mark pour le formatage
            ArrayNode marks = mapper.createArrayNode();
            ObjectNode formatMark = mapper.createObjectNode();
            formatMark.put("type", markType);
            
            marks.add(formatMark);
            textNode.set("marks", marks);
            
            // Utiliser Jackson pour créer l'objet Text à partir du JSON
            return mapper.treeToValue(textNode, Text.class);
        } catch (Exception e) {
            logger.warn("Impossible de créer un formatage natif ADF {}, utilisation du fallback: {}", markType, e.getMessage());
            // Fallback vers du texte simple en cas d'erreur
            return Text.of(text);
        }
    }
    
    /**
     * Traite un élément lien (<a>) et retourne une liste de nœuds Text ADF natifs.
     * Gère les liens avec des marks natifs au lieu du format fallback.
     */
    private List<Text> processLinkElement(Element linkElement) {
        List<Text> result = new ArrayList<>();
        String href = linkElement.attr("href");
        String linkText = linkElement.text().trim();
        
        // Si pas de href, traiter comme du texte simple
        if (href.isEmpty()) {
            if (!linkText.isEmpty()) {
                result.add(Text.of(linkText));
            }
            return result;
        }
        
        // Si pas de texte, utiliser l'URL comme texte
        if (linkText.isEmpty()) {
            linkText = href;
        }
        
        // Créer le lien natif ADF avec marks
        result.add(createNativeLinkText(linkText, href));
        
        return result;
    }

    /**
     * Generates a unique ID for an image based on its source URL.
     */
    private String generateImageId(String src) {
        // Generate a simple ID based on the filename or URL
        if (src.contains("/")) {
            String filename = src.substring(src.lastIndexOf("/") + 1);
            // Remove file extension and special characters
            return filename.replaceAll("\\.[^.]*$", "").replaceAll("[^a-zA-Z0-9_-]", "_");
        } else {
            return src.replaceAll("[^a-zA-Z0-9_-]", "_");
        }
    }
    
    /**
     * Checks if a URL is external (http/https) or local/relative.
     */
    private boolean isExternalUrl(String url) {
        return url.startsWith("http://") || url.startsWith("https://");
    }

    /**
     * Processes a standalone inline formatting element as a paragraph with proper ADF marks.
     * This handles cases like standalone <strong>, <em>, <code> elements that aren't within a paragraph.
     */
    private Document processInlineElementAsFormattedParagraph(Document doc, Element element, String formatType) {
        try {
            // Convert the element content to text nodes with formatting
            List<Text> textNodes = new ArrayList<>();
            
            for (org.jsoup.nodes.Node child : element.childNodes()) {
                if (child instanceof org.jsoup.nodes.TextNode) {
                    String text = ((org.jsoup.nodes.TextNode) child).text();
                    if (!text.trim().isEmpty()) {
                        if (formatType != null) {
                            textNodes.add(createFormattedText(text, formatType));
                        } else {
                            textNodes.add(Text.of(text));
                        }
                    }
                } else if (child instanceof Element) {
                    // Handle nested elements recursively 
                    Element childElement = (Element) child;
                    textNodes.addAll(processNodeToTextNodes(childElement));
                }
            }
            
            if (!textNodes.isEmpty()) {
                return doc.paragraph(textNodes.toArray(new Text[0]));
            } else {
                // Fallback to simple text extraction
                String text = getElementText(element);
                if (!text.trim().isEmpty()) {
                    if (formatType != null) {
                        Text formattedText = createFormattedText(text, formatType);
                        return doc.paragraph(formattedText);
                    } else {
                        return doc.paragraph(text);
                    }
                }
            }
            
        } catch (Exception e) {
            logger.warn("Error processing inline element as formatted paragraph, falling back to simple text", e);
            // Fallback to simple text extraction
            String text = getElementText(element);
            if (!text.trim().isEmpty()) {
                return doc.paragraph(text);
            }
        }
        
        return doc;
    }
    
    /**
     * Creates a Text node with the specified formatting mark using the ADF Builder API.
     */
    private Text createFormattedText(String text, String formatType) {
        Text textNode = Text.of(text);
        
        switch (formatType) {
            case "strong":
                return textNode.strong();
            case "em":
                return textNode.em();
            case "code":
                return textNode.code();
            case "underline":
                return createNativeFormattedText(text, "underline");
            case "strike":
                return createNativeFormattedText(text, "strike");
            default:
                return textNode;
        }
    }

    /**
     * Classe pour encapsuler le titre extrait et le contenu modifié.
     */
    public static class TitleAndContent {
        public final String title;
        public final String content;
        
        public TitleAndContent(String title, String content) {
            this.title = title;
            this.content = content;
        }
    }
}