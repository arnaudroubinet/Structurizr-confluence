package com.structurizr.confluence.processor;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests pour vérifier que la conversion AsciiDoc fonctionne correctement.
 */
public class AsciiDocConverterTest {
    
    @Test
    public void testBasicAsciiDocConversion() {
        AsciiDocConverter converter = new AsciiDocConverter();
        
        String asciiDocContent = """
            = Titre Principal
            
            == Section 1
            
            Ceci est un paragraphe avec du *texte en gras* et de l'_italique_.
            
            === Sous-section
            
            * Premier élément de liste
            * Deuxième élément de liste
            * Troisième élément
            
            [source,java]
            ----
            public class Example {
                public static void main(String[] args) {
                    System.out.println("Hello World!");
                }
            }
            ----
            """;
        
        String htmlResult = converter.convertToHtml(asciiDocContent, "Test Document");
        
        // Vérifications basiques
        assertNotNull(htmlResult, "Le résultat HTML ne devrait pas être null");
        assertFalse(htmlResult.trim().isEmpty(), "Le résultat HTML ne devrait pas être vide");
        
        // Afficher le résultat pour debug
        System.out.println("Contenu AsciiDoc original:");
        System.out.println(asciiDocContent);
        System.out.println("\nRésultat HTML converti:");
        System.out.println(htmlResult);
        System.out.println("\nLongueur du résultat: " + htmlResult.length());
        
        // Vérifier que la conversion AsciiDoc → HTML a fonctionné
        assertTrue(htmlResult.contains("Section 1"), "Le contenu des sections devrait être préservé");
        assertTrue(htmlResult.contains("<h2"), "Les sections == devraient être converties en h2");
        assertTrue(htmlResult.contains("<h3"), "Les sous-sections === devraient être converties en h3");
        assertTrue(htmlResult.contains("<strong>texte en gras</strong>"), "Le texte en gras devrait être converti");
        assertTrue(htmlResult.contains("<em>italique</em>"), "Le texte en italique devrait être converti");
        assertTrue(htmlResult.contains("<ul>"), "Les listes devraient être converties");
        assertTrue(htmlResult.contains("<li>"), "Les éléments de liste devraient être convertis");
        assertTrue(htmlResult.contains("<pre"), "Les blocs de code devraient être convertis");
        
        // Vérifier qu'il n'y a plus de syntaxe AsciiDoc brute
        assertFalse(htmlResult.contains("== Section"), "La syntaxe AsciiDoc brute ne devrait plus être présente");
        assertFalse(htmlResult.contains("=== Sous"), "La syntaxe AsciiDoc brute ne devrait plus être présente");
        assertFalse(htmlResult.contains("* Premier"), "La syntaxe de liste AsciiDoc ne devrait plus être présente");
    }
    
    @Test
    public void testEmptyContent() {
        AsciiDocConverter converter = new AsciiDocConverter();
        
        String result = converter.convertToHtml("", "Empty Test");
        assertEquals("", result, "Le contenu vide devrait retourner une chaîne vide");
        
        String nullResult = converter.convertToHtml(null, "Null Test");
        assertEquals("", nullResult, "Le contenu null devrait retourner une chaîne vide");
    }
    
    @Test
    public void testDiagramPlaceholder() {
        AsciiDocConverter converter = new AsciiDocConverter();
        
        String asciiDocWithDiagram = """
            = Documentation avec Diagramme
            
            Voici un diagramme :
            
            image::embed:SystemLandscape[]
            
            Et du texte après le diagramme.
            """;
        
        String htmlResult = converter.convertToHtml(asciiDocWithDiagram, "Diagram Test");
        
        assertNotNull(htmlResult);
        assertFalse(htmlResult.trim().isEmpty());
        
        // Afficher le résultat pour debug
        System.out.println("Contenu avec diagramme:");
        System.out.println(asciiDocWithDiagram);
        System.out.println("\nRésultat HTML:");
        System.out.println(htmlResult);
        
        // Vérifier que le placeholder de diagramme a été traité
        boolean containsDiagramRef = htmlResult.contains("SystemLandscape") || htmlResult.contains("DIAGRAM");
        assertTrue(containsDiagramRef, "Le nom du diagramme ou le placeholder devrait être préservé");
    }
}