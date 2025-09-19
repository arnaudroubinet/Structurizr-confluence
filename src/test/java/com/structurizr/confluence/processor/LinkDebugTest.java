package com.structurizr.confluence.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Debug test pour voir la structure ADF actuelle des liens.
 */
public class LinkDebugTest {

    private HtmlToAdfConverter converter;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        converter = new HtmlToAdfConverter();
        objectMapper = new ObjectMapper();
    }

    @Test
    void debugCurrentLinkStructure() throws Exception {
        String html = "<p>This architecture document follows the <a href=\"https://arc42.org/overview\">arc42</a> template.</p>";
        
        String adfJson = converter.convertToAdfJson(html, "Test Page");
        JsonNode adf = objectMapper.readTree(adfJson);
        
        System.out.println("=== STRUCTURE ADF ACTUELLE ===");
        System.out.println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(adf));
        
        JsonNode paragraph = adf.get("content").get(0);
        JsonNode paragraphContent = paragraph.get("content");
        
        System.out.println("\n=== DÉTAILS DU PARAGRAPHE ===");
        System.out.println("Nombre d'éléments de contenu: " + paragraphContent.size());
        
        for (int i = 0; i < paragraphContent.size(); i++) {
            JsonNode element = paragraphContent.get(i);
            System.out.println("Élément " + i + ": " + element.get("type").asText());
            if (element.has("text")) {
                System.out.println("  Texte: '" + element.get("text").asText() + "'");
            }
            if (element.has("marks")) {
                System.out.println("  Marks: " + element.get("marks").toString());
            }
        }
    }
}