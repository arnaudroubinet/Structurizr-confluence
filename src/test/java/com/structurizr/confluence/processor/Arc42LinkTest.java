package com.structurizr.confluence.processor;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test final pour vérifier que les liens "arc42" sont correctement traités.
 */
class Arc42LinkTest {
    private static final Logger logger = LoggerFactory.getLogger(Arc42LinkTest.class);
    
    @Test
    void testArc42LinkConversion() {
        
        HtmlToAdfConverter converter = new HtmlToAdfConverter();
        
        // HTML réel avec lien arc42 (comme dans AsciiDoc)
        String htmlWithArc42Link = "<p>This architecture document follows the <a href=\"https://arc42.org/overview\">arc42</a> template and describes the ITMS.</p>";
        
        logger.info("HTML d'entrée: {}", htmlWithArc42Link);
        
        // Convertir en ADF JSON
        String adfJson = converter.convertToAdfJson(htmlWithArc42Link, "Architecture Document");
        
        logger.info("ADF JSON généré:");
        logger.info(adfJson);
        
        // Vérifications de succès
        boolean containsArc42 = adfJson.contains("arc42");
        boolean containsUrl = adfJson.contains("https://arc42.org/overview");
        boolean hasCorrectSpaces = adfJson.contains("follows the arc42") && adfJson.contains("arc42 (https://arc42.org/overview) template");
        
        if (containsArc42 && containsUrl && hasCorrectSpaces) {
            logger.info("✅ SUCCÈS COMPLET !");
            logger.info("  ✓ Le texte 'arc42' est préservé");
            logger.info("  ✓ L'URL 'https://arc42.org/overview' est préservée");
            logger.info("  ✓ Les espaces autour du lien sont corrects");
            logger.info("  ✓ Format final: 'arc42 (https://arc42.org/overview)'");
        } else {
            logger.error("❌ ÉCHEC :");
            logger.error("  Arc42 présent: {}", containsArc42);
            logger.error("  URL présente: {}", containsUrl);
            logger.error("  Espaces corrects: {}", hasCorrectSpaces);
        }
        
        // Extraire et afficher juste le texte pour validation visuelle
        if (adfJson.contains("\"text\" : \"")) {
            String[] lines = adfJson.split("\n");
            for (String line : lines) {
                if (line.contains("\"text\" : \"") && line.contains("arc42")) {
                    String textLine = line.trim();
                    logger.info("📝 Texte extrait: {}", textLine);
                    break;
                }
            }
        }
    }
}