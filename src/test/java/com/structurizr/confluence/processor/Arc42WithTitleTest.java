package com.structurizr.confluence.processor;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test pour valider que l'extraction du titre fonctionne avec un contenu réaliste arc42.
 */
class Arc42WithTitleTest {
    private static final Logger logger = LoggerFactory.getLogger(Arc42WithTitleTest.class);
    
    @Test
    void testArc42ContentWithH1Title() {
        
        HtmlToAdfConverter converter = new HtmlToAdfConverter();
        
        // Contenu réaliste avec H1 titre + arc42 link
        String realisticContent = """
            <h1>Architecture Documentation</h1>
            <p>This architecture document follows the <a href="https://arc42.org/overview">arc42</a> template and describes the ITMS.</p>
            <h2>Introduction and Goals</h2>
            <p>The main goal of this system is to provide efficient IT management.</p>
            <h2>Architecture Overview</h2>
            <p>The system follows a microservices architecture pattern.</p>
            """;
        
        logger.info("Contenu d'entrée avec H1 et liens:");
        logger.info(realisticContent);
        
        // Extraire le titre
        String pageTitle = converter.extractPageTitleOnly(realisticContent);
        logger.info("Titre de page extrait: '{}'", pageTitle);
        
        // Convertir en ADF
        String adfJson = converter.convertToAdfJson(realisticContent, "Fallback Title");
        
        logger.info("ADF JSON généré:");
        logger.info(adfJson);
        
        // Vérifications
        assert pageTitle.equals("Architecture Documentation") : "Le titre devrait être 'Architecture Documentation'";
        assert !adfJson.contains("Architecture Documentation") : "Le titre H1 ne devrait pas être dans l'ADF";
        
        // Vérifier que le lien est maintenant natif avec des marks (et non plus en format fallback)
        assert adfJson.contains("\"text\" : \"arc42\"") : "Le texte du lien arc42 devrait être présent";
        assert adfJson.contains("\"type\" : \"link\"") : "Le lien devrait avoir un mark de type 'link'";
        assert adfJson.contains("\"href\" : \"https://arc42.org/overview\"") : "L'URL du lien devrait être dans les attrs";
        assert !adfJson.contains("arc42 (https://arc42.org/overview)") : "Le format fallback ne devrait plus être utilisé";
        
        assert adfJson.contains("\"level\" : 2") : "Les H2 devraient rester dans l'ADF";
        assert adfJson.contains("Introduction and Goals") : "Les sections H2 devraient être préservées";
        
        logger.info("✅ SUCCÈS : Titre extrait, H1 supprimé, liens natifs ADF générés, structure maintenue");
    }
}