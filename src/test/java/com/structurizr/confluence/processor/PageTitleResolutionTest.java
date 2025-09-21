package com.structurizr.confluence.processor;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires pour la résolution du titre de page (H1 prioritaire, puis fallback au nom de fichier)
 * en couvrant HTML et Markdown.
 */
class PageTitleResolutionTest {

    private final HtmlToAdfConverter converter = new HtmlToAdfConverter();

    @Test
    void html_with_valid_h1_uses_h1() {
        String html = "<h1>Valid Title</h1><p>Body</p>";
        String extracted = converter.extractPageTitleOnly(html);
        assertEquals("Valid Title", extracted);
    }

    @Test
    void html_with_whitespace_h1_falls_back() {
        String html = "<h1>   \t  </h1><p>Body</p>";
        String extracted = converter.extractPageTitleOnly(html);
        assertTrue(extracted == null || extracted.trim().isEmpty(), "Whitespace-only H1 must be ignored");
    }

    @Test
    void html_without_h1_falls_back() {
        String html = "<p>No title</p>";
        String extracted = converter.extractPageTitleOnly(html);
        assertNull(extracted);
    }

    @Test
    void first_h1_is_used_when_multiple_present() {
        String html = "<h1>First Title</h1><p>Intro</p><h1>Second Title</h1><p>More</p>";
        String extracted = converter.extractPageTitleOnly(html);
        assertEquals("First Title", extracted);
    }

    @Test
    void h1_text_is_trimmed_and_markup_removed() {
        String html = "<h1>  <strong>  Marked Title  </strong>  </h1><p>Body</p>";
        String extracted = converter.extractPageTitleOnly(html);
        assertEquals("Marked Title", extracted);
    }

    @Test
    void h1_with_leading_trailing_spaces_is_trimmed() {
        String html = "<h1>   Title with spaces   </h1><p>Body</p>";
        String extracted = converter.extractPageTitleOnly(html);
        assertEquals("Title with spaces", extracted);
    }
}
