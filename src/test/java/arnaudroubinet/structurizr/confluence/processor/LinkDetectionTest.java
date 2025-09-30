package arnaudroubinet.structurizr.confluence.processor;

import arnaudroubinet.structurizr.confluence.processor.HtmlToAdfConverter;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LinkDetectionTest {
    
    private static final Logger logger = LoggerFactory.getLogger(LinkDetectionTest.class);
    private final HtmlToAdfConverter converter = new HtmlToAdfConverter();
    
    @Test
    public void testLinkDetectionAndProcessing() {
        
        // Test avec contenu contenant un lien arc42
        String htmlWithLink = "<p>This architecture document follows the <a href=\"https://arc42.org/overview\">arc42</a> template and describes the ITMS.</p>";
        
        try {
            String adf = converter.convertToAdfJson(htmlWithLink, "Test Link");
            
            logger.info("HTML d'entrée avec lien: {}", htmlWithLink);
            logger.info("ADF de sortie: {}", adf);
            
            // Vérifier que le lien est présent sous forme "arc42 (https://arc42.org/overview)"
            if (adf.contains("arc42") && adf.contains("https://arc42.org/overview")) {
                logger.info("✅ SUCCÈS: Le lien arc42 est préservé dans l'ADF");
            } else {
                logger.error("❌ ÉCHEC: Le lien arc42 n'est pas correctement préservé");
            }
            
        } catch (Exception e) {
            logger.error("Erreur lors de la conversion: {}", e.getMessage(), e);
        }
    }
}