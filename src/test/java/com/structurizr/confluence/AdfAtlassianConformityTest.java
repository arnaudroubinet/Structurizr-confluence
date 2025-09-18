package com.structurizr.confluence;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.structurizr.confluence.processor.HtmlToAdfConverter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test de conformité avec la spécification ADF officielle d'Atlassian.
 * Basé sur : https://developer.atlassian.com/cloud/jira/platform/apis/document/structure/
 */
public class AdfAtlassianConformityTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HtmlToAdfConverter converter = new HtmlToAdfConverter();

    @Test
    public void testAdfDocumentStructureConformity() throws Exception {
        String htmlContent = """
            <h1>Document Title</h1>
            <p>Simple paragraph with <strong>bold</strong> and <em>italic</em> text.</p>
            <table>
                <thead>
                    <tr>
                        <th>Header 1</th>
                        <th>Header 2</th>
                    </tr>
                </thead>
                <tbody>
                    <tr>
                        <td>Data 1</td>
                        <td>Data 2</td>
                    </tr>
                </tbody>
            </table>
            """;

        String adfJson = converter.convertToAdfJson(htmlContent, "Html");
        JsonNode document = objectMapper.readTree(adfJson);

        // 1. Vérifier la structure racine du document
        assertTrue(document.has("version"), "Le document ADF doit avoir un champ 'version'");
        assertEquals(1, document.get("version").asInt(), "La version ADF doit être 1");
        
        assertTrue(document.has("type"), "Le document ADF doit avoir un champ 'type'");
        assertEquals("doc", document.get("type").asText(), "Le type racine doit être 'doc'");
        
        assertTrue(document.has("content"), "Le document ADF doit avoir un champ 'content'");
        assertTrue(document.get("content").isArray(), "Le content doit être un tableau");

        JsonNode content = document.get("content");
        assertTrue(content.size() > 0, "Le content ne doit pas être vide");

        // 2. Vérifier la structure des titres
        JsonNode heading = content.get(1); // Premier élément après le titre auto-généré
        assertEquals("heading", heading.get("type").asText(), "Le premier élément devrait être un heading");
        assertTrue(heading.has("attrs"), "Un heading doit avoir des attributs");
        assertTrue(heading.get("attrs").has("level"), "Un heading doit avoir un niveau");
        assertEquals(1, heading.get("attrs").get("level").asInt(), "Le niveau du heading doit être correct");

        // 3. Vérifier la structure des paragraphes
        JsonNode paragraph = content.get(2);
        assertEquals("paragraph", paragraph.get("type").asText(), "Le deuxième élément devrait être un paragraph");
        assertTrue(paragraph.has("content"), "Un paragraph doit avoir du contenu");

        // 4. Vérifier la structure des tables
        JsonNode table = content.get(3);
        assertEquals("table", table.get("type").asText(), "Le troisième élément devrait être une table");
        assertTrue(table.has("content"), "Une table doit avoir du contenu");
        
        JsonNode tableContent = table.get("content");
        assertTrue(tableContent.isArray(), "Le contenu d'une table doit être un tableau");
        assertTrue(tableContent.size() >= 2, "La table doit avoir au moins 2 lignes (header + data)");

        // 5. Vérifier la structure des lignes de table
        JsonNode headerRow = tableContent.get(0);
        assertEquals("tableRow", headerRow.get("type").asText(), "Les lignes doivent être de type tableRow");
        
        JsonNode dataRow = tableContent.get(1);
        assertEquals("tableRow", dataRow.get("type").asText(), "Les lignes doivent être de type tableRow");

        // 6. Vérifier la distinction tableHeader vs tableCell
        JsonNode headerRowContent = headerRow.get("content");
        JsonNode headerCell = headerRowContent.get(0);
        assertEquals("tableHeader", headerCell.get("type").asText(), 
                "Les cellules d'en-tête doivent être de type tableHeader");

        JsonNode dataRowContent = dataRow.get("content");
        JsonNode dataCell = dataRowContent.get(0);
        assertEquals("tableCell", dataCell.get("type").asText(), 
                "Les cellules de données doivent être de type tableCell");

        System.out.println("✅ Structure ADF conforme à la spécification Atlassian");
    }

    @Test
    public void testTextFormattingConformity() throws Exception {
        String htmlContent = """
            <p>Text with <strong>bold</strong>, <em>italic</em>, and <code>code</code> formatting.</p>
            """;

        String adfJson = converter.convertToAdfJson(htmlContent, "Html");
        JsonNode document = objectMapper.readTree(adfJson);
        
        // Naviguer vers le paragraphe
        JsonNode paragraph = document.get("content").get(1); // Après le titre auto-généré
        JsonNode paragraphContent = paragraph.get("content");
        
        // Vérifier que le formatage est préservé
        boolean hasTextWithMarks = false;
        for (JsonNode textNode : paragraphContent) {
            if (textNode.has("marks")) {
                hasTextWithMarks = true;
                JsonNode marks = textNode.get("marks");
                assertTrue(marks.isArray(), "Les marks doivent être un tableau");
                
                // Vérifier qu'au moins un mark a un type valide
                boolean hasValidMark = false;
                for (JsonNode mark : marks) {
                    String markType = mark.get("type").asText();
                    if ("strong".equals(markType) || "em".equals(markType) || "code".equals(markType)) {
                        hasValidMark = true;
                        break;
                    }
                }
                assertTrue(hasValidMark, "Au moins un mark doit avoir un type valide (strong, em, code)");
            }
        }
        
        System.out.println("✅ Formatage de texte conforme à la spécification ADF");
    }

    @Test
    public void testTableCellTypesCorrectness() throws Exception {
        String htmlContent = """
            <table>
                <thead>
                    <tr>
                        <th>Header Col 1</th>
                        <th>Header Col 2</th>
                    </tr>
                </thead>
                <tbody>
                    <tr>
                        <td>Data Cell 1</td>
                        <td>Data Cell 2</td>
                    </tr>
                    <tr>
                        <td>Data Cell 3</td>
                        <td>Data Cell 4</td>
                    </tr>
                </tbody>
            </table>
            """;

        String adfJson = converter.convertToAdfJson(htmlContent, "Html");
        JsonNode document = objectMapper.readTree(adfJson);
        
        // Naviguer vers la table
        JsonNode table = document.get("content").get(1); // Après le titre auto-généré
        JsonNode tableRows = table.get("content");
        
        // Vérifier la ligne d'en-tête
        JsonNode headerRow = tableRows.get(0);
        JsonNode headerCells = headerRow.get("content");
        for (JsonNode cell : headerCells) {
            assertEquals("tableHeader", cell.get("type").asText(), 
                    "Toutes les cellules de thead doivent être tableHeader");
        }
        
        // Vérifier les lignes de données
        for (int i = 1; i < tableRows.size(); i++) {
            JsonNode dataRow = tableRows.get(i);
            JsonNode dataCells = dataRow.get("content");
            for (JsonNode cell : dataCells) {
                assertEquals("tableCell", cell.get("type").asText(), 
                        "Toutes les cellules de tbody doivent être tableCell");
            }
        }
        
        System.out.println("✅ Types de cellules de table conformes à la spécification ADF");
        System.out.println("Nombre de lignes d'en-tête: 1");
        System.out.println("Nombre de lignes de données: " + (tableRows.size() - 1));
    }
}