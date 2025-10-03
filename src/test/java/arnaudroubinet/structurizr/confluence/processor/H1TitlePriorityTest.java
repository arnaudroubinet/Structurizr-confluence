package arnaudroubinet.structurizr.confluence.processor;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Tests pour la gestion prioritaire des titres H1 en début de page */
public class H1TitlePriorityTest {

  private HtmlToAdfConverter converter;

  @BeforeEach
  void setUp() {
    converter = new HtmlToAdfConverter();
  }

  @Test
  void testH1TitleHasPriorityOverProvidedTitle() {
    String htmlWithH1 = "<h1>Titre du H1</h1><p>Contenu de la page</p>";
    String providedTitle = "Titre fourni en paramètre";

    HtmlToAdfConverter.TitleAndContent result = converter.extractPageTitle(htmlWithH1);

    // Le H1 doit être extrait
    assertEquals("Titre du H1", result.title);

    // Le contenu ne doit plus contenir le H1
    assertFalse(result.content.contains("<h1>"));
    assertTrue(result.content.contains("<p>Contenu de la page</p>"));
  }

  @Test
  void testNoH1UsesProvidedTitle() {
    String htmlWithoutH1 = "<p>Contenu sans H1</p><h2>Sous-titre</h2>";

    HtmlToAdfConverter.TitleAndContent result = converter.extractPageTitle(htmlWithoutH1);

    // Aucun titre ne doit être extrait
    assertNull(result.title);

    // Le contenu doit être inchangé
    assertEquals(htmlWithoutH1, result.content);
  }

  @Test
  void testFirstH1IsUsedWhenMultipleH1Exist() {
    String htmlWithMultipleH1 = "<h1>Premier H1</h1><p>Contenu</p><h1>Deuxième H1</h1>";

    HtmlToAdfConverter.TitleAndContent result = converter.extractPageTitle(htmlWithMultipleH1);

    // Le premier H1 doit être utilisé
    assertEquals("Premier H1", result.title);

    // Seul le premier H1 doit être supprimé
    assertFalse(result.content.contains("Premier H1"));
    assertTrue(result.content.contains("Deuxième H1"));
  }

  @Test
  void testEmptyH1IsIgnored() {
    String htmlWithEmptyH1 = "<h1></h1><p>Contenu</p>";

    HtmlToAdfConverter.TitleAndContent result = converter.extractPageTitle(htmlWithEmptyH1);

    // Un H1 vide ne doit pas être utilisé
    assertNull(result.title);

    // Le contenu original doit être retourné
    assertEquals(htmlWithEmptyH1, result.content);
  }

  @Test
  void testWhitespaceOnlyH1IsIgnored() {
    String htmlWithWhitespaceH1 = "<h1>   </h1><p>Contenu</p>";

    HtmlToAdfConverter.TitleAndContent result = converter.extractPageTitle(htmlWithWhitespaceH1);

    // Un H1 avec seulement des espaces ne doit pas être utilisé
    assertNull(result.title);

    // Le contenu original doit être retourné
    assertEquals(htmlWithWhitespaceH1, result.content);
  }

  @Test
  void testH1WithSpecialCharactersIsPreserved() {
    String htmlWithSpecialChars =
        "<h1>ADR 1 - Record architecture decisions</h1><p>Date: 2016-02-12</p>";

    HtmlToAdfConverter.TitleAndContent result = converter.extractPageTitle(htmlWithSpecialChars);

    // Les caractères spéciaux doivent être préservés
    assertEquals("ADR 1 - Record architecture decisions", result.title);

    // Le contenu ne doit plus contenir le H1
    assertFalse(result.content.contains("<h1>"));
    assertTrue(result.content.contains("Date: 2016-02-12"));
  }

  @Test
  void testConvertToAdfJsonUsesH1Priority() {
    String htmlWithH1 = "<h1>Titre H1</h1><p>Contenu de test</p>";
    String providedTitle = "Titre fourni";

    String adfJson = converter.convertToAdfJson(htmlWithH1, providedTitle);

    // Vérifier que l'ADF ne contient pas le H1 dans le contenu
    // (le titre H1 est utilisé en métadonnée, pas dans le contenu)
    assertFalse(adfJson.contains("heading"));

    // Vérifier que l'ADF contient le contenu de test
    assertTrue(adfJson.contains("Contenu de test"));

    // Le titre H1 est utilisé en métadonnée (pas dans le contenu ADF)
    // mais on peut vérifier que la conversion s'est bien passée
    assertNotNull(adfJson);
    assertTrue(adfJson.contains("doc"));
    assertTrue(adfJson.contains("paragraph"));
  }

  @Test
  void testH1WithNestedElementsIsExtracted() {
    String htmlWithNestedH1 =
        "<h1>Titre avec <strong>gras</strong> et <em>italique</em></h1><p>Contenu</p>";

    HtmlToAdfConverter.TitleAndContent result = converter.extractPageTitle(htmlWithNestedH1);

    // Le texte du H1 doit être extrait sans les balises HTML
    assertEquals("Titre avec gras et italique", result.title);

    // Le contenu ne doit plus contenir le H1
    assertFalse(result.content.contains("<h1>"));
    assertTrue(result.content.contains("<p>Contenu</p>"));
  }
}
