package arnaudroubinet.structurizr.confluence.processor;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires pour vérifier l'extraction de titre à partir de contenu Markdown
 * après conversion simple vers HTML (simulateur minimal du convertisseur Markdown basique).
 */
class MarkdownTitleExtractionTest {

    private final HtmlToAdfConverter converter = new HtmlToAdfConverter();

    private String mdToHtmlBasic(String markdown) {
        if (markdown == null || markdown.trim().isEmpty()) return "";
        String html = markdown;
        html = html.replaceAll("(?m)^# (.+)$", "<h1>$1</h1>");
        html = html.replaceAll("(?m)^## (.+)$", "<h2>$1</h2>");
        html = html.replaceAll("(?m)^### (.+)$", "<h3>$1</h3>");
        html = html.replaceAll("(?m)^#### (.+)$", "<h4>$1</h4>");
        html = html.replaceAll("(?m)^##### (.+)$", "<h5>$1</h5>");
        html = html.replaceAll("(?m)^###### (.+)$", "<h6>$1</h6>");
        String[] paragraphs = html.split("\\n\\s*\\n");
        StringBuilder result = new StringBuilder();
        for (String paragraph : paragraphs) {
            String trimmed = paragraph.trim();
            if (!trimmed.isEmpty()) {
                if (!trimmed.matches(".*<h[1-6]>.*</h[1-6]>.*")) {
                    result.append("<p>").append(trimmed.replace("\n", " ")).append("</p>\n");
                } else {
                    result.append(trimmed).append("\n");
                }
            }
        }
        return result.toString();
    }

    @Test
    void markdown_h1_becomes_title() {
        String md = "# MD Title\n\nSome text";
        String html = mdToHtmlBasic(md);
        String extracted = converter.extractPageTitleOnly(html);
        assertEquals("MD Title", extracted);
    }

    @Test
    void markdown_no_h1_returns_null() {
        String md = "Some intro\n\n## Section";
        String html = mdToHtmlBasic(md);
        String extracted = converter.extractPageTitleOnly(html);
        assertNull(extracted);
    }

    @Test
    void markdown_h1_whitespace_ignored() {
        String md = "#    \n\nBody";
        String html = mdToHtmlBasic(md);
        String extracted = converter.extractPageTitleOnly(html);
        assertTrue(extracted == null || extracted.isBlank());
    }
}
