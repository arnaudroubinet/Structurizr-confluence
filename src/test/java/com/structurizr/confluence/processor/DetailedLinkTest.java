package com.structurizr.confluence.processor;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test détaillé pour analyser le traitement des liens étape par étape.
 */
class DetailedLinkTest {
    private static final Logger logger = LoggerFactory.getLogger(DetailedLinkTest.class);
    
    @Test
    void analyzeStepByStepLinkProcessing() {
        logger.info("=== ANALYSE DÉTAILLÉE DU TRAITEMENT DES LIENS ===");
        
        HtmlToAdfConverter converter = new HtmlToAdfConverter();
        
        // HTML simple avec lien
        String htmlWithLink = "<p>This architecture document follows the <a href=\"https://arc42.org/overview\">arc42</a> template and describes the ITMS.</p>";
        
        logger.info("HTML d'entrée: {}", htmlWithLink);
        
        // Convertir en ADF JSON
        String adfJson = converter.convertToAdfJson(htmlWithLink, "Debug Test");
        
        logger.info("ADF JSON de sortie:");
        logger.info(adfJson);
        
        // Vérifier les espaces dans le résultat
        if (adfJson.contains("thearc42")) {
            logger.error("❌ PROBLÈME DÉTECTÉ: Espaces manquants avant le lien");
        }
        
        if (adfJson.contains("overview)template")) {
            logger.error("❌ PROBLÈME DÉTECTÉ: Espaces manquants après le lien");
        }
        
        // Analyser le contenu pour trouver le texte exact
        if (adfJson.contains("\"text\"")) {
            String[] lines = adfJson.split("\n");
            for (String line : lines) {
                if (line.contains("\"text\"")) {
                    logger.info("Ligne de texte trouvée: {}", line.trim());
                }
            }
        }
    }
}