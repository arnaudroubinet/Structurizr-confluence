package com.structurizr.confluence.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for native ADF link generation with marks.
 */
public class NativeLinkTest {

    private HtmlToAdfConverter converter;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        converter = new HtmlToAdfConverter();
        objectMapper = new ObjectMapper();
    }

    @Test
    void testSimpleLinkConversion() throws Exception {
        // HTML avec un lien simple
        String html = "<p>Visit <a href=\"https://example.com\">our website</a> for more info.</p>";
        
        String adfJson = converter.convertToAdfJson(html, "Test Page");
        JsonNode adf = objectMapper.readTree(adfJson);
        
        // Vérifier la structure de base
        assertEquals("doc", adf.get("type").asText());
        assertEquals(1, adf.get("version").asInt());
        assertTrue(adf.has("content"));
        
        // Vérifier le contenu du paragraphe
        JsonNode paragraph = adf.get("content").get(0);
        assertEquals("paragraph", paragraph.get("type").asText());
        
        JsonNode paragraphContent = paragraph.get("content");
        assertEquals(3, paragraphContent.size()); // "Visit ", lien, " for more info."
        
        // Vérifier le premier texte
        JsonNode firstText = paragraphContent.get(0);
        assertEquals("text", firstText.get("type").asText());
        assertEquals("Visit ", firstText.get("text").asText());
        assertFalse(firstText.has("marks"));
        
        // Vérifier le lien (deuxième élément)
        JsonNode linkText = paragraphContent.get(1);
        assertEquals("text", linkText.get("type").asText());
        assertEquals("our website", linkText.get("text").asText());
        assertTrue(linkText.has("marks"));
        
        JsonNode marks = linkText.get("marks");
        assertEquals(1, marks.size());
        
        JsonNode linkMark = marks.get(0);
        assertEquals("link", linkMark.get("type").asText());
        assertEquals("https://example.com", linkMark.get("attrs").get("href").asText());
        
        // Vérifier le dernier texte
        JsonNode lastText = paragraphContent.get(2);
        assertEquals("text", lastText.get("type").asText());
        assertEquals(" for more info.", lastText.get("text").asText());
        assertFalse(lastText.has("marks"));
    }

    @Test
    void testLinkWithoutText() throws Exception {
        // Lien sans texte (devrait utiliser l'URL comme texte)
        String html = "<p>Check out <a href=\"https://example.com\"></a>!</p>";
        
        String adfJson = converter.convertToAdfJson(html, "Test Page");
        JsonNode adf = objectMapper.readTree(adfJson);
        
        JsonNode paragraph = adf.get("content").get(0);
        JsonNode paragraphContent = paragraph.get("content");
        
        // Le lien vide devrait être traité comme "https://example.com"
        JsonNode linkText = paragraphContent.get(1);
        assertEquals("text", linkText.get("type").asText());
        assertEquals("https://example.com", linkText.get("text").asText());
        assertTrue(linkText.has("marks"));
        
        JsonNode linkMark = linkText.get("marks").get(0);
        assertEquals("link", linkMark.get("type").asText());
        assertEquals("https://example.com", linkMark.get("attrs").get("href").asText());
    }

    @Test
    void testLinkWithoutHref() throws Exception {
        // Lien sans href (devrait être traité comme du texte simple)
        String html = "<p>This is <a>not a link</a> really.</p>";
        
        String adfJson = converter.convertToAdfJson(html, "Test Page");
        JsonNode adf = objectMapper.readTree(adfJson);
        
        JsonNode paragraph = adf.get("content").get(0);
        JsonNode paragraphContent = paragraph.get("content");
        
        // Devrait être traité comme un seul nœud texte (pas de lien)
        assertEquals(1, paragraphContent.size());
        JsonNode text = paragraphContent.get(0);
        assertEquals("text", text.get("type").asText());
        assertEquals("This is not a link really.", text.get("text").asText());
        assertFalse(text.has("marks"));
    }

    @Test
    void testMultipleLinksInParagraph() throws Exception {
        // Plusieurs liens dans le même paragraphe
        String html = "<p>Visit <a href=\"https://atlassian.com\">Atlassian</a> or <a href=\"https://github.com\">GitHub</a>.</p>";
        
        String adfJson = converter.convertToAdfJson(html, "Test Page");
        JsonNode adf = objectMapper.readTree(adfJson);
        
        JsonNode paragraph = adf.get("content").get(0);
        JsonNode paragraphContent = paragraph.get("content");
        
        assertEquals(5, paragraphContent.size()); // "Visit ", lien1, " or ", lien2, "."
        
        // Premier lien
        JsonNode link1 = paragraphContent.get(1);
        assertEquals("Atlassian", link1.get("text").asText());
        assertEquals("https://atlassian.com", link1.get("marks").get(0).get("attrs").get("href").asText());
        
        // Deuxième lien
        JsonNode link2 = paragraphContent.get(3);
        assertEquals("GitHub", link2.get("text").asText());
        assertEquals("https://github.com", link2.get("marks").get(0).get("attrs").get("href").asText());
    }

    @Test
    void testArc42LinkExample() throws Exception {
        // Test du cas réel mentionné par l'utilisateur
        String html = "<p>This architecture document follows the <a href=\"https://arc42.org/overview\">arc42</a> template.</p>";
        
        String adfJson = converter.convertToAdfJson(html, "Test Page");
        JsonNode adf = objectMapper.readTree(adfJson);
        
        JsonNode paragraph = adf.get("content").get(0);
        JsonNode paragraphContent = paragraph.get("content");
        
        assertEquals(3, paragraphContent.size());
        
        // Vérifier que le lien "arc42" est correct et cliquable
        JsonNode linkText = paragraphContent.get(1);
        assertEquals("text", linkText.get("type").asText());
        assertEquals("arc42", linkText.get("text").asText());
        assertTrue(linkText.has("marks"));
        
        JsonNode linkMark = linkText.get("marks").get(0);
        assertEquals("link", linkMark.get("type").asText());
        assertEquals("https://arc42.org/overview", linkMark.get("attrs").get("href").asText());
        
        // Vérifier qu'il n'y a pas de format fallback "(https://...)"
        String fullText = paragraphContent.get(0).get("text").asText() + 
                         paragraphContent.get(1).get("text").asText() + 
                         paragraphContent.get(2).get("text").asText();
        assertFalse(fullText.contains("(https://"), "Le lien ne devrait pas contenir l'URL en format fallback");
        assertEquals("This architecture document follows the arc42 template.", fullText);
    }
}