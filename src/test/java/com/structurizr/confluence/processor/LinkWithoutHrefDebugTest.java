package com.structurizr.confluence.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Debug test pour voir ce qui se passe avec les liens sans href.
 */
public class LinkWithoutHrefDebugTest {

    private HtmlToAdfConverter converter;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        converter = new HtmlToAdfConverter();
        objectMapper = new ObjectMapper();
    }

    @Test
    void debugLinkWithoutHrefStructure() throws Exception {
        String html = "<p>This is <a>not a link</a> really.</p>";
        
        String adfJson = converter.convertToAdfJson(html, "Test Page");
        JsonNode adf = objectMapper.readTree(adfJson);
        
        System.out.println("=== STRUCTURE ADF LIEN SANS HREF ===");
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