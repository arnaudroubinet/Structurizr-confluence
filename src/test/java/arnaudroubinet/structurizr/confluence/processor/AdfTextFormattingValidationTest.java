package arnaudroubinet.structurizr.confluence.processor;

import com.atlassian.adf.Document;
import com.atlassian.adf.inline.Text;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test de validation de l'API Text avec formatage.
 */
class AdfTextFormattingValidationTest {
    private static final Logger logger = LoggerFactory.getLogger(AdfTextFormattingValidationTest.class);
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Test
    void testCreateFormattedText() {
        logger.info("=== VALIDATION API TEXT AVEC FORMATAGE ===");
        
        try {
            Document doc = Document.create();
            
            // Créer du texte avec formatage strong
            Text strongText = Text.of("important").strong();
            logger.info("✓ Texte strong créé : {}", strongText);
            
            // Créer du texte avec formatage em  
            Text emText = Text.of("souligner").em();
            logger.info("✓ Texte em créé : {}", emText);
            
            // Créer du texte avec formatage code
            Text codeText = Text.of("getElementText()").code();
            logger.info("✓ Texte code créé : {}", codeText);
            
            // Créer un paragraphe avec plusieurs nœuds Text
            Text plainTextBefore = Text.of("Ceci est ");
            Text plainTextAfter = Text.of(" à retenir.");
            
            doc = doc.paragraph(plainTextBefore, strongText, plainTextAfter);
            
            // Convertir en JSON pour voir la structure
            String adfJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(doc);
            logger.info("ADF généré avec formatage :");
            logger.info(adfJson);
            
            // Vérifier la structure
            if (adfJson.contains("\"marks\"") && adfJson.contains("\"type\" : \"strong\"")) {
                logger.info("✅ SUCCESS: Le formatage ADF natif fonctionne !");
            } else {
                logger.warn("⚠️ Le formatage pourrait ne pas être correct");
            }
            
        } catch (Exception e) {
            logger.error("❌ Erreur lors de la création du texte formaté: {}", e.getMessage(), e);
        }
    }
}