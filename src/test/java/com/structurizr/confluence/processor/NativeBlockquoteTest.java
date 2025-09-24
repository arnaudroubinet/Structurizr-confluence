package com.structurizr.confluence.processor;

import com.atlassian.adf.Document;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tests to verify que les blockquotes HTML sont convertis en blockquote ADF natifs.
 */
public class NativeBlockquoteTest {
    private static final Logger logger = LoggerFactory.getLogger(NativeBlockquoteTest.class);
    private HtmlToAdfConverter converter;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    public void setUp() {
        converter = new HtmlToAdfConverter();
    }

    @Test
    public void testSimpleBlockquoteConversion() throws JsonProcessingException {
        
        String htmlContent = "<blockquote><p>Ceci est une citation importante pour illustrer un point clé.</p></blockquote>";
        
        Document adfDocument = converter.convertToAdf(htmlContent, "Test Blockquote");
        String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(adfDocument);
        
        logger.info("HTML d'entrée: {}", htmlContent);
        logger.info("ADF généré: {}", json);
        
        // Vérifier que le type est "blockquote" et non un paragraphe avec préfixe
        assert json.contains("\"type\" : \"blockquote\"") : "Le type devrait être blockquote";
        assert !json.contains("\"> \"") : "Ne devrait pas contenir de préfixe manuel >";
        assert json.contains("Ceci est une citation importante") : "Devrait contenir le texte de la citation";
    }

    @Test
    public void testBlockquoteWithInlineFormatting() throws JsonProcessingException {
        
        String htmlContent = "<blockquote><p>Citation avec <strong>texte important</strong> et <em>emphasis</em>.</p></blockquote>";
        
        Document adfDocument = converter.convertToAdf(htmlContent, "Test Blockquote Formatting");
        String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(adfDocument);
        
        logger.info("HTML d'entrée: {}", htmlContent);
        logger.info("ADF généré: {}", json);
        
        // Vérifier que le type est "blockquote"
        assert json.contains("\"type\" : \"blockquote\"") : "Le type devrait être blockquote";
        
        // Vérifier que le formatage inline est préservé avec des marks
        assert json.contains("\"type\" : \"strong\"") : "Devrait contenir des marks strong";
        assert json.contains("\"type\" : \"em\"") : "Devrait contenir des marks em";
        
        // Vérifier le contenu
        assert json.contains("Citation avec") : "Devrait contenir le texte de la citation";
        assert json.contains("texte important") : "Devrait contenir le texte formaté en gras";
        assert json.contains("emphasis") : "Devrait contenir le texte formaté en italique";
    }
    
    @Test
    public void testMultipleParagraphsInBlockquote() throws JsonProcessingException {
        
        String htmlContent = "<blockquote><p>Premier paragraphe de la citation.</p><p>Deuxième paragraphe avec plus de détails.</p></blockquote>";
        
        Document adfDocument = converter.convertToAdf(htmlContent, "Test Multi-Paragraph Blockquote");
        String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(adfDocument);
        
        logger.info("HTML d'entrée: {}", htmlContent);
        logger.info("ADF généré: {}", json);
        
        // Vérifier que le type est "blockquote"
        assert json.contains("\"type\" : \"blockquote\"") : "Le type devrait être blockquote";
        
        // Vérifier que les deux paragraphes sont présents
        assert json.contains("Premier paragraphe") : "Devrait contenir le premier paragraphe";
        assert json.contains("Deuxième paragraphe") : "Devrait contenir le deuxième paragraphe";
        
        // Compter les paragraphes dans la citation
        int paragraphCount = json.split("\"type\" : \"paragraph\"").length - 1;
        assert paragraphCount >= 2 : "Devrait contenir au moins 2 paragraphes dans la citation";
    }
    
    @Test
    public void testSimpleTextBlockquote() throws JsonProcessingException {
        
        String htmlContent = "<blockquote>Citation simple sans balise paragraphe.</blockquote>";
        
        Document adfDocument = converter.convertToAdf(htmlContent, "Test Simple Text Blockquote");
        String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(adfDocument);
        
        logger.info("HTML d'entrée: {}", htmlContent);
        logger.info("ADF généré: {}", json);
        
        // Vérifier que le type est "blockquote"
        assert json.contains("\"type\" : \"blockquote\"") : "Le type devrait être blockquote";
        assert json.contains("Citation simple sans balise") : "Devrait contenir le texte de la citation";
    }
    
    @Test
    public void testComparisonWithOldFormat() throws JsonProcessingException {
        
        String htmlContent = "<blockquote><p>Cette citation ne devrait plus avoir de préfixe manuel.</p></blockquote>";
        
        Document adfDocument = converter.convertToAdf(htmlContent, "Test Old Format Comparison");
        String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(adfDocument);
        
        logger.info("HTML d'entrée: {}", htmlContent);
        logger.info("ADF généré: {}", json);
        
        // Vérifier qu'on n'utilise plus l'ancien format avec préfixe
        assert json.contains("\"type\" : \"blockquote\"") : "Devrait utiliser le type blockquote natif";
        assert !json.contains("\"> \"") : "Ne devrait plus contenir de préfixe manuel >";
        assert !json.contains("&gt; ") : "Ne devrait plus contenir de préfixe encodé";
        
        // Vérifier que le contenu est proprement structuré
        assert json.contains("Cette citation ne devrait plus") : "Devrait contenir le texte sans préfixe";
    }
}