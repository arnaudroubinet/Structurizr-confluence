package com.structurizr.confluence.processor;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test unitaire minimal pour vérifier qu'un H1 AsciiDoc ("= Title")
 * est bien géré après conversion en HTML par AsciiDocConverter.convertToHtml.
 * NOTE: Ce test isole l'extraction titre/HTML en simulant la sortie HTML attendue.
 */
class AsciiDocTitleExtractionTest {

    private final HtmlToAdfConverter converter = new HtmlToAdfConverter();

    @Test
    void asciidoc_h1_becomes_title_after_html_conversion() {
        // Simulation de sortie HTML typique d'Asciidoctor pour "= Title" en tête
        String html = "<h1>AD Title</h1>\n<p>Du contenu</p>";
        String extracted = converter.extractPageTitleOnly(html);
        assertEquals("AD Title", extracted);
    }

    @Test
    void asciidoc_missing_h1_returns_null() {
        String html = "<p>Pas de H1 ici</p>";
        String extracted = converter.extractPageTitleOnly(html);
        assertNull(extracted);
    }
}
