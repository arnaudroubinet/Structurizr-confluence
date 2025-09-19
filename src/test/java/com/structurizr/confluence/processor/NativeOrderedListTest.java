package com.structurizr.confluence.processor;

import com.atlassian.adf.Document;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tests pour vérifier que les listes ordonnées HTML sont converties en orderedList ADF natifs.
 */
public class NativeOrderedListTest {
    private static final Logger logger = LoggerFactory.getLogger(NativeOrderedListTest.class);
    private HtmlToAdfConverter converter;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    public void setUp() {
        converter = new HtmlToAdfConverter();
    }

    @Test
    public void testSimpleOrderedListConversion() throws JsonProcessingException {
        logger.info("=== TEST SIMPLE ORDERED LIST CONVERSION ===");
        
        String htmlContent = "<ol><li>Premier item</li><li>Deuxième item</li><li>Troisième item</li></ol>";
        
        Document adfDocument = converter.convertToAdf(htmlContent, "Test Ordered List");
        String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(adfDocument);
        
        logger.info("HTML d'entrée: {}", htmlContent);
        logger.info("ADF généré: {}", json);
        
        // Vérifier que le type est "orderedList" et non "bulletList"
        assert json.contains("\"type\" : \"orderedList\"") : "Le type devrait être orderedList";
        assert !json.contains("\"type\" : \"bulletList\"") : "Ne devrait pas contenir bulletList";
        assert !json.contains("1. ") : "Ne devrait pas contenir de numéros manuels";
        assert json.contains("Premier item") : "Devrait contenir le texte du premier item";
        assert json.contains("Deuxième item") : "Devrait contenir le texte du deuxième item";
        assert json.contains("Troisième item") : "Devrait contenir le texte du troisième item";
    }

    @Test
    public void testOrderedListWithInlineFormatting() throws JsonProcessingException {
        logger.info("=== TEST ORDERED LIST WITH INLINE FORMATTING ===");
        
        String htmlContent = "<ol><li>Item avec <strong>gras</strong></li><li>Item avec <em>italique</em></li><li>Item avec <code>code</code></li></ol>";
        
        Document adfDocument = converter.convertToAdf(htmlContent, "Test Ordered List Formatting");
        String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(adfDocument);
        
        logger.info("HTML d'entrée: {}", htmlContent);
        logger.info("ADF généré: {}", json);
        
        // Vérifier que le type est "orderedList"
        assert json.contains("\"type\" : \"orderedList\"") : "Le type devrait être orderedList";
        
        // Vérifier que le formatage inline est préservé avec des marks
        assert json.contains("\"type\" : \"strong\"") : "Devrait contenir des marks strong";
        assert json.contains("\"type\" : \"em\"") : "Devrait contenir des marks em";
        assert json.contains("\"type\" : \"code\"") : "Devrait contenir des marks code";
        
        // Vérifier le contenu
        assert json.contains("Item avec") : "Devrait contenir le texte des items";
        assert json.contains("gras") : "Devrait contenir le texte formaté en gras";
        assert json.contains("italique") : "Devrait contenir le texte formaté en italique";
        assert json.contains("code") : "Devrait contenir le texte formaté en code";
    }
    
    @Test
    public void testComparisonWithBulletList() throws JsonProcessingException {
        logger.info("=== TEST COMPARISON ORDERED VS BULLET LIST ===");
        
        String orderedHtml = "<ol><li>Item ordonné 1</li><li>Item ordonné 2</li></ol>";
        String bulletHtml = "<ul><li>Item bullet 1</li><li>Item bullet 2</li></ul>";
        
        Document orderedDoc = converter.convertToAdf(orderedHtml, "Test Ordered");
        Document bulletDoc = converter.convertToAdf(bulletHtml, "Test Bullet");
        
        String orderedJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(orderedDoc);
        String bulletJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(bulletDoc);
        
        logger.info("Ordered List JSON: {}", orderedJson);
        logger.info("Bullet List JSON: {}", bulletJson);
        
        // Vérifier que les types sont différents
        assert orderedJson.contains("\"type\" : \"orderedList\"") : "Ordered list devrait avoir le type orderedList";
        assert bulletJson.contains("\"type\" : \"bulletList\"") : "Bullet list devrait avoir le type bulletList";
        
        // Vérifier que la structure des items est similaire
        assert orderedJson.contains("\"type\" : \"listItem\"") : "Ordered list devrait contenir des listItem";
        assert bulletJson.contains("\"type\" : \"listItem\"") : "Bullet list devrait contenir des listItem";
    }
}