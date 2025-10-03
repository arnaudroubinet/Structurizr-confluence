package arnaudroubinet.structurizr.confluence.processor;

import static org.junit.jupiter.api.Assertions.*;

import com.atlassian.adf.Document;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test to validate that all Structurizr formatting is preserved in Confluence. This addresses the
 * issue: "Respect de la mise en forme structurizr dans confluence"
 */
class StructurizrFormattingValidationTest {
  private static final Logger logger =
      LoggerFactory.getLogger(StructurizrFormattingValidationTest.class);

  private final ObjectMapper objectMapper = new ObjectMapper();
  private final HtmlToAdfConverter converter = new HtmlToAdfConverter();

  @Test
  void testStructurizrWorkspaceFormattingPreservation() {
    logger.info("=== VALIDATION FORMATAGE STRUCTURIZR ===");

    // Test content similar to the demo workspace
    String htmlContent =
        "<div class=\"sect1\">"
            + "<h2 id=\"_introduction_and_goals\">Introduction and Goals</h2>"
            + "<div class=\"sectionbody\">"
            + "<div class=\"paragraph\">"
            + "<p>This architecture document follows the <a href=\"https://arc42.org/overview\">arc42</a> template and describes the ITMS (Instant Ticket Manager System) and the platforms with which it interacts. The <code>itms-workspace.dsl</code> (Structurizr DSL) is the source of truth for actors, external systems, containers and relationships.</p>"
            + "</div>"
            + "<div class=\"sect2\">"
            + "<h3 id=\"_vision\">Vision</h3>"
            + "<div class=\"paragraph\">"
            + "<p>ITMS enables <strong>secure, auditable and resilient</strong> management of instant ticket lifecycle operations while integrating with <em>identity providers</em>, external retail platform, data &amp; audit infrastructures.</p>"
            + "</div>"
            + "</div>"
            + "</div>"
            + "</div>";

    try {
      Document doc = converter.convertToAdf(htmlContent, "Structurizr Formatting Test");
      String adfJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(doc);

      logger.info("ADF généré pour le contenu Structurizr:");
      logger.info(adfJson);

      // Vérifications des éléments de formatage
      assertTrue(
          adfJson.contains("\"type\" : \"link\""),
          "Les liens doivent être préservés avec des marks link natives");
      assertTrue(
          adfJson.contains("\"href\" : \"https://arc42.org/overview\""),
          "L'URL du lien doit être préservée");
      assertTrue(adfJson.contains("\"type\" : \"code\""), "Le formatage code doit être préservé");
      assertTrue(
          adfJson.contains("\"type\" : \"strong\""), "Le formatage strong doit être préservé");
      assertTrue(adfJson.contains("\"type\" : \"em\""), "Le formatage em doit être préservé");

      // Vérifier que le texte n'est pas converti en fallback
      assertFalse(
          adfJson.contains("arc42 (https://arc42.org/overview)"),
          "Les liens ne doivent pas être convertis en format fallback");

      // Vérifier la structure des headings
      assertTrue(adfJson.contains("\"type\" : \"heading\""), "Les titres doivent être préservés");
      assertTrue(
          adfJson.contains("\"level\" : 2"), "Les niveaux de titre H2 doivent être préservés");
      assertTrue(
          adfJson.contains("\"level\" : 3"), "Les niveaux de titre H3 doivent être préservés");

      logger.info(
          "✅ Tous les éléments de formatage Structurizr sont correctement préservés dans Confluence ADF!");

    } catch (Exception e) {
      logger.error("Erreur lors de la validation du formatage Structurizr", e);
      fail("La conversion du formatage Structurizr a échoué: " + e.getMessage());
    }
  }

  @Test
  void testComplexInlineFormattingMix() {

    // Test avec formatage imbriqué et multiple
    String htmlWithComplexFormatting =
        "<p>The system uses <code>container-level</code> deployment with <strong>zero-trust boundaries</strong> and "
            + "<em>explicit egress policies</em>. Configuration is stored in <a href=\"https://s3.amazonaws.com\">S3</a> "
            + "with <code>immutable artifacts</code> and <strong><em>version control</em></strong>.</p>";

    try {
      Document doc = converter.convertToAdf(htmlWithComplexFormatting, "Complex Formatting Test");
      String adfJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(doc);

      logger.info("ADF pour formatage complexe:");
      logger.info(adfJson);

      // Compter les différents types de marks
      int linkCount = countOccurrences(adfJson, "\"type\" : \"link\"");
      int codeCount = countOccurrences(adfJson, "\"type\" : \"code\"");
      int strongCount = countOccurrences(adfJson, "\"type\" : \"strong\"");
      int emCount = countOccurrences(adfJson, "\"type\" : \"em\"");

      logger.info(
          "Marks détectées - Links: {}, Code: {}, Strong: {}, Em: {}",
          linkCount,
          codeCount,
          strongCount,
          emCount);

      assertEquals(1, linkCount, "Doit avoir exactement 1 lien");
      assertEquals(2, codeCount, "Doit avoir exactement 2 éléments code");
      assertTrue(strongCount >= 1, "Doit avoir au moins 1 élément strong");
      assertTrue(emCount >= 1, "Doit avoir au moins 1 élément em");

      logger.info("✅ Le formatage inline complexe est correctement géré!");

    } catch (Exception e) {
      logger.error("Erreur lors du test de formatage complexe", e);
      fail("Le test de formatage complexe a échoué: " + e.getMessage());
    }
  }

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
