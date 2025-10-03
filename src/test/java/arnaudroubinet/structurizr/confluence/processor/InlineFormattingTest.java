package arnaudroubinet.structurizr.confluence.processor;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tests TDD pour le formatage inline avec les marks ADF.
 * Ces tests définissent le comportement attendu pour <strong>, <em>, <code>, etc.
 */
class InlineFormattingTest {
  private static final Logger logger = LoggerFactory.getLogger(InlineFormattingTest.class);

  private final HtmlToAdfConverter converter = new HtmlToAdfConverter();

  @Test
  void testStrongTagShouldCreateStrongMark() {

    String htmlWithStrong = "<p>Ceci est <strong>important</strong> à retenir.</p>";
    String adfJson = converter.convertToAdfJson(htmlWithStrong, "Test Strong");

    logger.info("HTML d'entrée: {}", htmlWithStrong);
    logger.info("ADF généré: {}", adfJson);

    // Vérifications attendues pour les marks strong
    assertTrue(
        adfJson.contains("\"type\" : \"strong\""), "Devrait contenir un mark de type 'strong'");
    assertTrue(
        adfJson.contains("\"text\" : \"important\""), "Devrait contenir le texte 'important'");
    assertTrue(adfJson.contains("\"marks\""), "Devrait utiliser le système de marks ADF");

    // Vérifier que ce n'est PAS du texte simple
    assertFalse(
        adfJson.contains("Ceci est important à retenir"),
        "Ne devrait PAS être converti en texte simple");
  }

  @Test
  void testEmTagShouldCreateEmMark() {

    String htmlWithEm = "<p>Il faut <em>souligner</em> ce point.</p>";
    String adfJson = converter.convertToAdfJson(htmlWithEm, "Test Em");

    logger.info("HTML d'entrée: {}", htmlWithEm);
    logger.info("ADF généré: {}", adfJson);

    assertTrue(adfJson.contains("\"type\" : \"em\""), "Devrait contenir un mark de type 'em'");
    assertTrue(
        adfJson.contains("\"text\" : \"souligner\""), "Devrait contenir le texte 'souligner'");
    assertTrue(adfJson.contains("\"marks\""), "Devrait utiliser le système de marks ADF");
  }

  @Test
  void testCodeTagShouldCreateCodeMark() {

    String htmlWithCode =
        "<p>Utilisez la fonction <code>getElementText()</code> pour extraire le texte.</p>";
    String adfJson = converter.convertToAdfJson(htmlWithCode, "Test Code");

    logger.info("HTML d'entrée: {}", htmlWithCode);
    logger.info("ADF généré: {}", adfJson);

    assertTrue(adfJson.contains("\"type\" : \"code\""), "Devrait contenir un mark de type 'code'");
    assertTrue(
        adfJson.contains("\"text\" : \"getElementText()\""),
        "Devrait contenir le texte 'getElementText()'");
    assertTrue(adfJson.contains("\"marks\""), "Devrait utiliser le système de marks ADF");
  }

  @Test
  void testUnderlineTagShouldCreateUnderlineMark() {

    String htmlWithU = "<p>Ce texte est <u>souligné</u> pour emphasis.</p>";
    String adfJson = converter.convertToAdfJson(htmlWithU, "Test Underline");

    logger.info("HTML d'entrée: {}", htmlWithU);
    logger.info("ADF généré: {}", adfJson);

    assertTrue(
        adfJson.contains("\"type\" : \"underline\""),
        "Devrait contenir un mark de type 'underline'");
    assertTrue(adfJson.contains("\"text\" : \"souligné\""), "Devrait contenir le texte 'souligné'");
  }

  @Test
  void testStrikeTagShouldCreateStrikeMark() {

    String htmlWithStrike = "<p>Ce texte est <s>barré</s> car obsolète.</p>";
    String adfJson = converter.convertToAdfJson(htmlWithStrike, "Test Strike");

    logger.info("HTML d'entrée: {}", htmlWithStrike);
    logger.info("ADF généré: {}", adfJson);

    assertTrue(
        adfJson.contains("\"type\" : \"strike\""), "Devrait contenir un mark de type 'strike'");
    assertTrue(adfJson.contains("\"text\" : \"barré\""), "Devrait contenir le texte 'barré'");
  }

  @Test
  void testMultipleMarksInSameParagraph() {

    String htmlWithMultiple =
        "<p>Texte avec <strong>gras</strong>, <em>italique</em> et <code>code</code>.</p>";
    String adfJson = converter.convertToAdfJson(htmlWithMultiple, "Test Multiple");

    logger.info("HTML d'entrée: {}", htmlWithMultiple);
    logger.info("ADF généré: {}", adfJson);

    assertTrue(adfJson.contains("\"type\" : \"strong\""), "Devrait contenir mark strong");
    assertTrue(adfJson.contains("\"type\" : \"em\""), "Devrait contenir mark em");
    assertTrue(adfJson.contains("\"type\" : \"code\""), "Devrait contenir mark code");

    // Vérifier que les trois textes sont présents
    assertTrue(adfJson.contains("\"text\" : \"gras\""));
    assertTrue(adfJson.contains("\"text\" : \"italique\""));
    assertTrue(adfJson.contains("\"text\" : \"code\""));
  }

  @Test
  void testNestedMarksHandling() {

    String htmlWithNested =
        "<p>Texte avec <strong>gras et <em>italique imbriqué</em></strong>.</p>";
    String adfJson = converter.convertToAdfJson(htmlWithNested, "Test Nested");

    logger.info("HTML d'entrée: {}", htmlWithNested);
    logger.info("ADF généré: {}", adfJson);

    // Pour le formatage imbriqué, on devrait avoir des marks multiples sur le même texte
    // ou des nœuds de texte séparés avec leurs marks respectifs
    assertTrue(adfJson.contains("\"marks\""), "Devrait utiliser le système de marks");
    assertTrue(
        adfJson.contains("\"text\" : \"italique imbriqué\""), "Devrait contenir le texte imbriqué");
  }
}
