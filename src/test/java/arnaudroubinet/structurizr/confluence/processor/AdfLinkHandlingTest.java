package arnaudroubinet.structurizr.confluence.processor;

import arnaudroubinet.structurizr.confluence.processor.HtmlToAdfConverter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests de gestion des liens dans la conversion HTML vers ADF.
 * 
 * Ces tests vérifient que les liens HTML sont correctement convertis en structures ADF
 * conformes à la spécification officielle d'Atlassian.
 */
public class AdfLinkHandlingTest {

    private static final Logger logger = LoggerFactory.getLogger(AdfLinkHandlingTest.class);
    
    private HtmlToAdfConverter converter;

    @BeforeEach
    public void setUp() {
        converter = new HtmlToAdfConverter();
    }

    @Test
    public void testInlineLinkConversion() {
        String html = "<p>Consultez la <a href=\"https://developer.atlassian.com\">documentation officielle</a> pour plus d'informations.</p>";
        
        logger.info("Test de conversion de lien inline");
        logger.info("HTML d'entrée: {}", html);
        
        String adfJson = converter.convertToAdfJson(html, "Test");
        
        logger.info("ADF généré: {}", adfJson);
        
        // Vérifier que la structure ADF contient un vrai lien et non du texte simple
        assertTrue(adfJson.contains("\"type\"") && adfJson.contains("\"text\""), "Le texte doit être présent");
        assertTrue(adfJson.contains("\"marks\""), "Les marques de lien doivent être présentes");
        assertTrue(adfJson.contains("\"type\"") && adfJson.contains("\"link\""), "Le type de marque doit être 'link'");
        assertTrue(adfJson.contains("\"href\"") && adfJson.contains("https://developer.atlassian.com"), "L'URL doit être préservée");
        assertTrue(adfJson.contains("documentation officielle"), "Le texte du lien doit être préservé");
        
        // Vérifier qu'il n'y a pas de format Markdown ou de texte simple avec URL
        assertFalse(adfJson.contains("]("), "Ne doit pas contenir de syntaxe Markdown");
        assertFalse(adfJson.contains("(https://"), "Ne doit pas contenir d'URL en texte simple");
    }

    @Test
    public void testStandaloneLinkConversion() {
        String html = "<a href=\"https://structurizr.com\">Site officiel de Structurizr</a>";
        
        logger.info("Test de conversion de lien autonome");
        logger.info("HTML d'entrée: {}", html);
        
        String adfJson = converter.convertToAdfJson(html, "Test");
        
        logger.info("ADF généré: {}", adfJson);
        
        // Vérifier la structure ADF pour un lien autonome
        assertTrue(adfJson.contains("\"type\"") && adfJson.contains("\"paragraph\""), "Doit être dans un paragraphe");
        assertTrue(adfJson.contains("\"type\"") && adfJson.contains("\"text\""), "Le texte doit être présent");
        assertTrue(adfJson.contains("\"marks\""), "Les marques de lien doivent être présentes");
        assertTrue(adfJson.contains("\"type\"") && adfJson.contains("\"link\""), "Le type de marque doit être 'link'");
        assertTrue(adfJson.contains("https://structurizr.com"), "L'URL doit être préservée");
        assertTrue(adfJson.contains("Site officiel de Structurizr"), "Le texte du lien doit être préservé");
    }

    @Test
    public void testMultipleLinksInParagraph() {
        String html = "<p>Voir <a href=\"https://atlassian.com\">Atlassian</a> et <a href=\"https://structurizr.com\">Structurizr</a> pour plus d'infos.</p>";
        
        logger.info("Test de conversion de liens multiples");
        logger.info("HTML d'entrée: {}", html);
        
        String adfJson = converter.convertToAdfJson(html, "Test");
        
        logger.info("ADF généré: {}", adfJson);
        
        // Vérifier que les deux liens sont correctement convertis
        assertTrue(adfJson.contains("https://atlassian.com"), "Premier lien doit être préservé");
        assertTrue(adfJson.contains("https://structurizr.com"), "Deuxième lien doit être préservé");
        assertTrue(adfJson.contains("Atlassian"), "Premier texte de lien doit être préservé");
        assertTrue(adfJson.contains("Structurizr"), "Deuxième texte de lien doit être préservé");
        
        // Vérifier la structure ADF des liens
        assertTrue(adfJson.contains("\"marks\""), "Les marques de lien doivent être présentes");
        assertTrue(adfJson.contains("\"type\"") && adfJson.contains("\"link\""), "Le type de marque doit être 'link'");
    }

    @Test
    public void testLinkWithEmptyHref() {
        String html = "<a href=\"\">Lien vide</a>";
        
        logger.info("Test de conversion de lien avec href vide");
        logger.info("HTML d'entrée: {}", html);
        
        String adfJson = converter.convertToAdfJson(html, "Test");
        
        logger.info("ADF généré: {}", adfJson);
        
        // Un lien avec href vide doit être traité comme du texte simple
        assertTrue(adfJson.contains("Lien vide"), "Le texte doit être préservé");
        // Mais il ne doit pas y avoir de marque de lien
        assertFalse(adfJson.contains("\"type\"") && adfJson.contains("\"link\""), "Ne doit pas contenir de marque de lien");
    }

    @Test
    public void testLinkWithNoHref() {
        String html = "<a>Lien sans href</a>";
        
        logger.info("Test de conversion de lien sans attribut href");
        logger.info("HTML d'entrée: {}", html);
        
        String adfJson = converter.convertToAdfJson(html, "Test");
        
        logger.info("ADF généré: {}", adfJson);
        
        // Un lien sans href doit être traité comme du texte simple
        assertTrue(adfJson.contains("Lien sans href"), "Le texte doit être préservé");
        assertFalse(adfJson.contains("\"type\"") && adfJson.contains("\"link\""), "Ne doit pas contenir de marque de lien");
    }

    /**
     * Compte le nombre d'occurrences d'une sous-chaîne dans une chaîne.
     */
    private int countOccurrences(String text, String pattern) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(pattern, index)) != -1) {
            count++;
            index += pattern.length();
        }
        return count;
    }
}