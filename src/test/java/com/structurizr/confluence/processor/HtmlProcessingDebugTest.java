package com.structurizr.confluence.processor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test pour identifier précisément le problème de troncature dans la conversion HTML→ADF.
 */
public class HtmlProcessingDebugTest {
    
    private static final Logger logger = LoggerFactory.getLogger(HtmlProcessingDebugTest.class);
    
    private HtmlToAdfConverter htmlToAdfConverter;
    
    @BeforeEach
    void setUp() {
        htmlToAdfConverter = new HtmlToAdfConverter();
    }
    
    @Test
    void debugSimpleHtmlStructure() {
        // HTML simple pour isoler le problème
        String simpleHtml = """
            <div class="sect1">
                <h2>Premier Titre</h2>
                <div class="sectionbody">
                    <div class="paragraph">
                        <p>Premier paragraphe avec du contenu.</p>
                    </div>
                    <div class="sect2">
                        <h3>Sous-titre</h3>
                        <div class="paragraph">
                            <p>Deuxième paragraphe avec plus de contenu.</p>
                        </div>
                    </div>
                </div>
            </div>
            """;
        
        logger.info("HTML d'entrée:\\n{}", simpleHtml);
        
        String adfJson = htmlToAdfConverter.convertToAdfJson(simpleHtml, "Test Simple");
        
        logger.info("ADF de sortie:\\n{}", adfJson);
        
        // Analyser le résultat
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode adfTree = mapper.readTree(adfJson);
            
            com.fasterxml.jackson.databind.JsonNode content = adfTree.get("content");
            if (content != null && content.isArray()) {
                logger.info("Nombre d'éléments ADF générés: {}", content.size());
                
                for (int i = 0; i < content.size(); i++) {
                    com.fasterxml.jackson.databind.JsonNode element = content.get(i);
                    String type = element.get("type").asText();
                    logger.info("  [{}] Type: {}", i, type);
                    
                    if ("paragraph".equals(type) || type.startsWith("heading")) {
                        com.fasterxml.jackson.databind.JsonNode elemContent = element.get("content");
                        if (elemContent != null && elemContent.isArray() && elemContent.size() > 0) {
                            com.fasterxml.jackson.databind.JsonNode firstText = elemContent.get(0);
                            if (firstText.has("text")) {
                                String text = firstText.get("text").asText();
                                logger.info("       Contenu: {}", text);
                            }
                        }
                    }
                }
            }
            
        } catch (Exception e) {
            logger.error("Erreur lors de l'analyse: {}", e.getMessage(), e);
        }
    }
    
    @Test
    void debugMultipleParagraphs() {
        // Test avec plusieurs paragraphes simples
        String multiParagraphHtml = """
            <p>Premier paragraphe.</p>
            <p>Deuxième paragraphe.</p>
            <p>Troisième paragraphe.</p>
            """;
        
        logger.info("HTML d'entrée:\\n{}", multiParagraphHtml);
        
        String adfJson = htmlToAdfConverter.convertToAdfJson(multiParagraphHtml, "Test Multi");
        
        logger.info("ADF de sortie:\\n{}", adfJson);
        
        // Compter les paragraphes dans le résultat
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode adfTree = mapper.readTree(adfJson);
            
            com.fasterxml.jackson.databind.JsonNode content = adfTree.get("content");
            if (content != null && content.isArray()) {
                int paragraphCount = 0;
                for (int i = 0; i < content.size(); i++) {
                    com.fasterxml.jackson.databind.JsonNode element = content.get(i);
                    if ("paragraph".equals(element.get("type").asText())) {
                        paragraphCount++;
                    }
                }
                logger.info("Paragraphes attendus: 3, trouvés: {}", paragraphCount);
                
                if (paragraphCount != 3) {
                    logger.error("PROBLÈME DÉTECTÉ: Perte de contenu!");
                }
            }
            
        } catch (Exception e) {
            logger.error("Erreur lors de l'analyse: {}", e.getMessage(), e);
        }
    }
}