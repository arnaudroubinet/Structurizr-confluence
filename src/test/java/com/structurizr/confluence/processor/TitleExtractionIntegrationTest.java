package com.structurizr.confluence.processor;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test d'intégration pour vérifier que l'extraction de titre fonctionne 
 * correctement dans le contexte complet de l'export Confluence.
 */
class TitleExtractionIntegrationTest {
    private static final Logger logger = LoggerFactory.getLogger(TitleExtractionIntegrationTest.class);

    @Test
    void testCompleteExportProcessWithTitleExtraction() {
        logger.info("=== TEST : PROCESSUS COMPLET D'EXPORT AVEC EXTRACTION DE TITRE ===");
        
        HtmlToAdfConverter converter = new HtmlToAdfConverter();
        
        // Simuler le contenu HTML d'un fichier .adoc avec titre
        String htmlContent = """
            <h1>ITMS : Instant Ticket Manager System - Introduction and Goals</h1>
            <p>This document describes the introduction and goals of the ITMS system.</p>
            <h2>Requirements Overview</h2>
            <p>The main requirements are:</p>
            <ul>
                <li>Fast ticket processing</li>
                <li>Reliable system architecture</li>
            </ul>
            <h2>Quality Goals</h2>
            <p>Performance and scalability are key quality goals.</p>
            """;
        
        // Simuler le nom de fichier original (comme dans l'exporter)
        String originalFileName = "01_introduction_and_goals.adoc";
        String branchName = "main";
        
        logger.info("Nom de fichier original: '{}'", originalFileName);
        logger.info("Contenu HTML d'entrée:\n{}", htmlContent);
        
        // Étape 1: Extraire le titre du contenu HTML (comme fait dans ConfluenceExporter)
        String extractedTitle = converter.extractPageTitleOnly(htmlContent);
        String actualTitle = extractedTitle != null && !extractedTitle.trim().isEmpty() ? extractedTitle : originalFileName;
        
        logger.info("Titre extrait du H1: '{}'", extractedTitle);
        logger.info("Titre final utilisé: '{}'", actualTitle);
        
        // Étape 2: Convertir vers ADF (comme fait dans ConfluenceExporter)
        String adfJson = converter.convertToAdfJson(htmlContent, actualTitle);
        
        // Étape 3: Créer le titre de page final (comme fait dans ConfluenceExporter)
        String pageTitle = branchName + " - " + actualTitle;
        
        logger.info("Titre de page Confluence final: '{}'", pageTitle);
        logger.info("ADF JSON généré:\n{}", adfJson);
        
        // Vérifications
        assert extractedTitle != null : "Le titre devrait être extrait du H1";
        assert "ITMS : Instant Ticket Manager System - Introduction and Goals".equals(extractedTitle) : 
               "Le titre extrait devrait correspondre au contenu du H1";
        assert pageTitle.equals("main - ITMS : Instant Ticket Manager System - Introduction and Goals") : 
               "Le titre de page final devrait inclure le branch et le titre extrait";
        
        // Vérifier que le H1 n'est plus dans le contenu ADF
        assert !adfJson.contains("\"text\" : \"ITMS : Instant Ticket Manager System - Introduction and Goals\"") : 
               "Le titre H1 ne devrait plus apparaître dans le contenu ADF";
        
        // Vérifier que les autres éléments sont présents
        assert adfJson.contains("\"text\" : \"Requirements Overview\"") : 
               "Le H2 devrait être présent dans l'ADF";
        assert adfJson.contains("\"text\" : \"This document describes the introduction and goals of the ITMS system.\"") : 
               "Le paragraphe principal devrait être présent";
        
        logger.info("✅ SUCCÈS : Le processus complet d'export avec extraction de titre fonctionne correctement");
        logger.info("   - Titre extrait du H1 au lieu du nom de fichier");
        logger.info("   - H1 supprimé du contenu pour éviter la duplication");
        logger.info("   - Structure ADF correctement générée");
    }
    
    @Test
    void testFallbackToFileNameWhenNoH1() {
        logger.info("=== TEST : FALLBACK AU NOM DE FICHIER QUAND AUCUN H1 ===");
        
        HtmlToAdfConverter converter = new HtmlToAdfConverter();
        
        // Contenu sans H1
        String htmlContent = """
            <p>This document has no title.</p>
            <h2>Section 1</h2>
            <p>Some content here.</p>
            """;
        
        String originalFileName = "02_architecture_constraints.adoc";
        String branchName = "develop";
        
        logger.info("Nom de fichier original: '{}'", originalFileName);
        logger.info("Contenu HTML (sans H1):\n{}", htmlContent);
        
        // Extraire le titre (devrait être null)
        String extractedTitle = converter.extractPageTitleOnly(htmlContent);
        String actualTitle = extractedTitle != null && !extractedTitle.trim().isEmpty() ? extractedTitle : originalFileName;
        
        // Créer le titre de page final
        String pageTitle = branchName + " - " + actualTitle;
        
        logger.info("Titre extrait: '{}'", extractedTitle);
        logger.info("Titre final (fallback): '{}'", actualTitle);
        logger.info("Titre de page Confluence: '{}'", pageTitle);
        
        // Vérifications
        assert extractedTitle == null || extractedTitle.trim().isEmpty() : 
               "Aucun titre ne devrait être extrait car il n'y a pas de H1";
        assert actualTitle.equals(originalFileName) : 
               "Le titre final devrait être le nom de fichier en fallback";
        assert pageTitle.equals("develop - 02_architecture_constraints.adoc") : 
               "Le titre de page devrait utiliser le nom de fichier";
        
        logger.info("✅ SUCCÈS : Le fallback au nom de fichier fonctionne correctement");
    }
}