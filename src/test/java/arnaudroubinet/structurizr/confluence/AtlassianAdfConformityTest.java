package arnaudroubinet.structurizr.confluence;

import static org.junit.jupiter.api.Assertions.*;

import arnaudroubinet.structurizr.confluence.processor.HtmlToAdfConverter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

/**
 * Test de conformité ADF selon la spécification officielle Atlassian. Basé sur:
 * https://developer.atlassian.com/cloud/jira/platform/apis/document/structure/
 */
public class AtlassianAdfConformityTest {

  private final ObjectMapper objectMapper = new ObjectMapper();
  private final HtmlToAdfConverter converter = new HtmlToAdfConverter();

  @Test
  public void testAdfDocumentStructureConformity() throws Exception {
    String htmlContent =
        """
            <h1>Document Title</h1>
            <p>Simple paragraph with <strong>bold text</strong> and <em>italic text</em>.</p>
            <table>
                <thead>
                    <tr>
                        <th>Header 1</th>
                        <th>Header 2</th>
                    </tr>
                </thead>
                <tbody>
                    <tr>
                        <td>Cell 1</td>
                        <td>Cell 2</td>
                    </tr>
                </tbody>
            </table>
            """;

    String adfJson = converter.convertToAdfJson(htmlContent, "Html");
    JsonNode rootNode = objectMapper.readTree(adfJson);

    // Validation de la structure racine du document selon spec Atlassian
    validateRootDocumentStructure(rootNode);

    // Validation des éléments spécifiques
    JsonNode content = rootNode.get("content");
    assertNotNull(content, "Le document doit avoir un array 'content'");
    assertTrue(content.isArray(), "Le 'content' doit être un array");

    boolean foundTable = false;
    for (JsonNode element : content) {
      if ("table".equals(element.get("type").asText())) {
        validateTableStructure(element);
        foundTable = true;
      } else if ("heading".equals(element.get("type").asText())) {
        validateHeadingStructure(element);
      } else if ("paragraph".equals(element.get("type").asText())) {
        validateParagraphStructure(element);
      }
    }

    assertTrue(foundTable, "Une table doit être présente dans le document");
    System.out.println("✅ Structure ADF conforme à la spécification Atlassian");
  }

  private void validateRootDocumentStructure(JsonNode rootNode) {
    // Validation selon la spec:
    // https://developer.atlassian.com/cloud/jira/platform/apis/document/structure/
    assertEquals(1, rootNode.get("version").asInt(), "La version du document ADF doit être 1");
    assertEquals("doc", rootNode.get("type").asText(), "Le type racine doit être 'doc'");
    assertTrue(rootNode.has("content"), "Le document doit avoir un champ 'content'");
    assertTrue(rootNode.get("content").isArray(), "Le 'content' doit être un array");
  }

  private void validateTableStructure(JsonNode tableNode) {
    // Validation structure table selon spec Atlassian
    assertEquals("table", tableNode.get("type").asText(), "Le type doit être 'table'");

    JsonNode content = tableNode.get("content");
    assertNotNull(content, "La table doit avoir un 'content'");
    assertTrue(content.isArray(), "Le content de la table doit être un array");

    // Validation des lignes de table
    for (JsonNode row : content) {
      assertEquals(
          "tableRow", row.get("type").asText(), "Chaque ligne doit être de type 'tableRow'");

      JsonNode rowContent = row.get("content");
      assertNotNull(rowContent, "Chaque ligne doit avoir un 'content'");
      assertTrue(rowContent.isArray(), "Le content de la ligne doit être un array");

      // Validation des cellules
      for (JsonNode cell : rowContent) {
        String cellType = cell.get("type").asText();
        assertTrue(
            cellType.equals("tableHeader") || cellType.equals("tableCell"),
            "Les cellules doivent être de type 'tableHeader' ou 'tableCell'");

        JsonNode cellContent = cell.get("content");
        assertNotNull(cellContent, "Chaque cellule doit avoir un 'content'");
        assertTrue(cellContent.isArray(), "Le content de la cellule doit être un array");
      }
    }
  }

  private void validateHeadingStructure(JsonNode headingNode) {
    assertEquals("heading", headingNode.get("type").asText(), "Le type doit être 'heading'");

    JsonNode attrs = headingNode.get("attrs");
    assertNotNull(attrs, "Le heading doit avoir des 'attrs'");
    assertTrue(attrs.has("level"), "Le heading doit avoir un niveau");

    int level = attrs.get("level").asInt();
    assertTrue(level >= 1 && level <= 6, "Le niveau de heading doit être entre 1 et 6");

    JsonNode content = headingNode.get("content");
    assertNotNull(content, "Le heading doit avoir un 'content'");
    assertTrue(content.isArray(), "Le content du heading doit être un array");
  }

  private void validateParagraphStructure(JsonNode paragraphNode) {
    assertEquals("paragraph", paragraphNode.get("type").asText(), "Le type doit être 'paragraph'");

    JsonNode content = paragraphNode.get("content");
    assertNotNull(content, "Le paragraph doit avoir un 'content'");
    assertTrue(content.isArray(), "Le content du paragraph doit être un array");

    // Validation des éléments de texte dans le paragraphe
    for (JsonNode textNode : content) {
      String nodeType = textNode.get("type").asText();
      assertTrue(
          nodeType.equals("text") || nodeType.equals("strong") || nodeType.equals("em"),
          "Les éléments de paragraphe doivent être du texte ou du formatage");

      if ("text".equals(nodeType)) {
        assertTrue(textNode.has("text"), "Les nœuds text doivent avoir un champ 'text'");
      }
    }
  }

  @Test
  public void testComplexTableConformity() throws Exception {
    // Test avec une table plus complexe incluant une distinction claire header/data
    String complexHtmlTable =
        """
            <table>
                <thead>
                    <tr>
                        <th>Service</th>
                        <th>Status</th>
                        <th>Description</th>
                        <th>Actions</th>
                    </tr>
                </thead>
                <tbody>
                    <tr>
                        <td>API Gateway</td>
                        <td><strong>Active</strong></td>
                        <td>Handles all <em>external</em> requests</td>
                        <td>Monitor</td>
                    </tr>
                    <tr>
                        <td>Database</td>
                        <td><strong>Active</strong></td>
                        <td>Primary data store</td>
                        <td>Backup</td>
                    </tr>
                </tbody>
            </table>
            """;

    String adfJson = converter.convertToAdfJson(complexHtmlTable, "Html");
    JsonNode rootNode = objectMapper.readTree(adfJson);

    // Trouver la table
    JsonNode content = rootNode.get("content");
    JsonNode tableNode = null;
    for (JsonNode element : content) {
      if ("table".equals(element.get("type").asText())) {
        tableNode = element;
        break;
      }
    }

    assertNotNull(tableNode, "Une table doit être présente");

    // Vérifier que la table a le bon nombre de lignes
    JsonNode tableContent = tableNode.get("content");
    assertEquals(3, tableContent.size(), "La table doit avoir 3 lignes (1 header + 2 data)");

    // Vérifier que chaque ligne a 4 cellules
    for (JsonNode row : tableContent) {
      JsonNode rowContent = row.get("content");
      assertEquals(4, rowContent.size(), "Chaque ligne doit avoir 4 cellules");
    }

    // Vérifier la distinction header/data
    JsonNode firstRow = tableContent.get(0);
    JsonNode firstRowContent = firstRow.get("content");

    // La première ligne doit avoir des tableHeader
    for (JsonNode cell : firstRowContent) {
      assertEquals(
          "tableHeader",
          cell.get("type").asText(),
          "Les cellules de la première ligne (thead) doivent être de type tableHeader");
    }

    // Les lignes suivantes doivent avoir des tableCell
    for (int i = 1; i < tableContent.size(); i++) {
      JsonNode dataRow = tableContent.get(i);
      JsonNode dataRowContent = dataRow.get("content");
      for (JsonNode cell : dataRowContent) {
        assertEquals(
            "tableCell",
            cell.get("type").asText(),
            "Les cellules des lignes de données (tbody) doivent être de type tableCell");
      }
    }

    System.out.println("✅ Table complexe conforme à la spécification ADF");
    System.out.println("Structure de la table générée:");
    System.out.println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(tableNode));
  }
}
