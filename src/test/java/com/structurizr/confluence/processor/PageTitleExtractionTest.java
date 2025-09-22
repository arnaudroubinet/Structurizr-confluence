package com.structurizr.confluence.processor;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test to verify l'extraction du titre de page depuis le premier H1.
 */
class PageTitleExtractionTest {
    private static final Logger logger = LoggerFactory.getLogger(PageTitleExtractionTest.class);
    
    @Test
    void testExtractTitleFromFirstH1() {
        
        HtmlToAdfConverter converter = new HtmlToAdfConverter();
        
        // HTML avec H1 en première position
        String htmlWithH1 = "<h1>Architecture Overview</h1><p>This document describes the architecture.</p><h2>Section 1</h2><p>Content here.</p>";
        
        logger.info("HTML d'entrée: {}", htmlWithH1);
        
        // Extraire le titre uniquement
        String extractedTitle = converter.extractPageTitleOnly(htmlWithH1);
        logger.info("Titre extrait: '{}'", extractedTitle);
        
        // Extraire titre et contenu
        HtmlToAdfConverter.TitleAndContent result = converter.extractPageTitle(htmlWithH1);
        logger.info("Titre: '{}'", result.title);
        logger.info("Contenu sans H1: {}", result.content);
        
        // Vérifications
        assertEquals("Architecture Overview", extractedTitle);
        assertEquals("Architecture Overview", result.title);
        assertFalse(result.content.contains("<h1>Architecture Overview</h1>"));
        assertTrue(result.content.contains("<p>This document describes the architecture.</p>"));
        assertTrue(result.content.contains("<h2>Section 1</h2>"));
        
        logger.info("✅ Titre extrait correctement et supprimé du contenu");
    }
    
    @Test
    void testNoH1InContent() {
        
        HtmlToAdfConverter converter = new HtmlToAdfConverter();
        
        // HTML sans H1
        String htmlWithoutH1 = "<p>This document has no title.</p><h2>Section 1</h2><p>Content here.</p>";
        
        logger.info("HTML d'entrée: {}", htmlWithoutH1);
        
        // Extraire le titre
        String extractedTitle = converter.extractPageTitleOnly(htmlWithoutH1);
        HtmlToAdfConverter.TitleAndContent result = converter.extractPageTitle(htmlWithoutH1);
        
        logger.info("Titre extrait: '{}'", extractedTitle);
        logger.info("Contenu: {}", result.content);
        
        // Vérifications
        assertNull(extractedTitle);
        assertNull(result.title);
        assertEquals(htmlWithoutH1, result.content);
        
        logger.info("✅ Aucun titre extrait, contenu inchangé");
    }
    
    @Test
    void testConvertToAdfJsonWithH1Title() {
        
        HtmlToAdfConverter converter = new HtmlToAdfConverter();
        
        // HTML avec H1 qui doit devenir le titre de page
        String htmlWithH1 = "<h1>My Page Title</h1><p>This is the content after the title.</p>";
        
        logger.info("HTML d'entrée: {}", htmlWithH1);
        
        // Convertir en ADF JSON
        String adfJson = converter.convertToAdfJson(htmlWithH1, "Fallback Title");
        
        logger.info("ADF JSON généré:");
        logger.info(adfJson);
        
        // Vérifications
        assertFalse(adfJson.contains("\"level\" : 1")); // Pas de H1 dans le contenu ADF
        assertFalse(adfJson.contains("My Page Title")); // Le titre n'est pas dans le contenu ADF
        assertTrue(adfJson.contains("This is the content after the title")); // Le contenu reste
        
        // Le contenu ADF doit commencer directement par le paragraphe
        assertTrue(adfJson.contains("\"type\" : \"paragraph\""));
        
        logger.info("✅ Le H1 a été extrait comme titre et supprimé du contenu ADF");
    }
    
    @Test
    void testMultipleH1OnlyFirstExtracted() {
        
        HtmlToAdfConverter converter = new HtmlToAdfConverter();
        
        // HTML avec plusieurs H1
        String htmlWithMultipleH1 = "<h1>Main Title</h1><p>Content</p><h1>Another Title</h1><p>More content</p>";
        
        logger.info("HTML d'entrée: {}", htmlWithMultipleH1);
        
        HtmlToAdfConverter.TitleAndContent result = converter.extractPageTitle(htmlWithMultipleH1);
        
        logger.info("Titre extrait: '{}'", result.title);
        logger.info("Contenu modifié: {}", result.content);
        
        // Vérifications
        assertEquals("Main Title", result.title);
        assertFalse(result.content.contains("<h1>Main Title</h1>"));
        assertTrue(result.content.contains("<h1>Another Title</h1>")); // Le second H1 reste
        
        logger.info("✅ Seul le premier H1 a été extrait, le second reste dans le contenu");
    }
}