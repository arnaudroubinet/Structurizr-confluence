package com.structurizr.confluence.processor;

/**
 * Test simple pour déboguer la conversion AsciiDoc
 */
public class AsciiDocDebugTest {
    
    public static void main(String[] args) {
        System.out.println("=== Test de conversion AsciiDoc ===");
        
        try {
            AsciiDocConverter converter = new AsciiDocConverter();
            
            // Test très simple
            String simpleContent = "= Titre Simple\n\nCeci est un paragraphe.";
            System.out.println("Contenu AsciiDoc:");
            System.out.println("'" + simpleContent + "'");
            
            String result = converter.convertToHtml(simpleContent, "Test Simple");
            System.out.println("\nRésultat HTML:");
            System.out.println("'" + result + "'");
            System.out.println("Longueur: " + result.length());
            
            // Test avec du contenu vide
            String emptyResult = converter.convertToHtml("", "Test Vide");
            System.out.println("\nRésultat vide:");
            System.out.println("'" + emptyResult + "'");
            
            // Test avec null
            String nullResult = converter.convertToHtml(null, "Test Null");
            System.out.println("\nRésultat null:");
            System.out.println("'" + nullResult + "'");
            
            converter.close();
            
        } catch (Exception e) {
            System.err.println("Erreur lors du test:");
            e.printStackTrace();
        }
    }
}