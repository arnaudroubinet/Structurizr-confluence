package com.structurizr.confluence.processor;

import com.atlassian.adf.Document;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

/**
 * Test pour créer des tables ADF natives en utilisant la structure JSON directe.
 */
public class NativeAdfTableTest {

    @Test
    public void testCreateNativeAdfTable() throws Exception {
        System.out.println("=== Test création table ADF native ===");
        
        ObjectMapper mapper = new ObjectMapper();
        
        // Créer une structure ADF avec table native
        ObjectNode doc = mapper.createObjectNode();
        doc.put("version", 1);
        doc.put("type", "doc");
        
        ArrayNode content = mapper.createArrayNode();
        
        // Ajouter un titre
        ObjectNode heading = mapper.createObjectNode();
        heading.put("type", "heading");
        ObjectNode headingAttrs = mapper.createObjectNode();
        headingAttrs.put("level", 2);
        heading.set("attrs", headingAttrs);
        ArrayNode headingContent = mapper.createArrayNode();
        ObjectNode headingText = mapper.createObjectNode();
        headingText.put("type", "text");
        headingText.put("text", "Table Test");
        headingContent.add(headingText);
        heading.set("content", headingContent);
        content.add(heading);
        
        // Créer une table native ADF
        ObjectNode table = mapper.createObjectNode();
        table.put("type", "table");
        ArrayNode tableContent = mapper.createArrayNode();
        
        // Row 1 (header)
        ObjectNode row1 = mapper.createObjectNode();
        row1.put("type", "tableRow");
        ArrayNode row1Content = mapper.createArrayNode();
        
        // Header cell 1
        ObjectNode cell1 = mapper.createObjectNode();
        cell1.put("type", "tableHeader");
        ArrayNode cell1Content = mapper.createArrayNode();
        ObjectNode cell1Para = mapper.createObjectNode();
        cell1Para.put("type", "paragraph");
        ArrayNode cell1ParaContent = mapper.createArrayNode();
        ObjectNode cell1Text = mapper.createObjectNode();
        cell1Text.put("type", "text");
        cell1Text.put("text", "Stakeholder");
        cell1ParaContent.add(cell1Text);
        cell1Para.set("content", cell1ParaContent);
        cell1Content.add(cell1Para);
        cell1.set("content", cell1Content);
        row1Content.add(cell1);
        
        // Header cell 2
        ObjectNode cell2 = mapper.createObjectNode();
        cell2.put("type", "tableHeader");
        ArrayNode cell2Content = mapper.createArrayNode();
        ObjectNode cell2Para = mapper.createObjectNode();
        cell2Para.put("type", "paragraph");
        ArrayNode cell2ParaContent = mapper.createArrayNode();
        ObjectNode cell2Text = mapper.createObjectNode();
        cell2Text.put("type", "text");
        cell2Text.put("text", "Role / Interest");
        cell2ParaContent.add(cell2Text);
        cell2Para.set("content", cell2ParaContent);
        cell2Content.add(cell2Para);
        cell2.set("content", cell2Content);
        row1Content.add(cell2);
        
        // Header cell 3
        ObjectNode cell3 = mapper.createObjectNode();
        cell3.put("type", "tableHeader");
        ArrayNode cell3Content = mapper.createArrayNode();
        ObjectNode cell3Para = mapper.createObjectNode();
        cell3Para.put("type", "paragraph");
        ArrayNode cell3ParaContent = mapper.createArrayNode();
        ObjectNode cell3Text = mapper.createObjectNode();
        cell3Text.put("type", "text");
        cell3Text.put("text", "Key Expectations");
        cell3ParaContent.add(cell3Text);
        cell3Para.set("content", cell3ParaContent);
        cell3Content.add(cell3Para);
        cell3.set("content", cell3Content);
        row1Content.add(cell3);
        
        row1.set("content", row1Content);
        tableContent.add(row1);
        
        // Row 2 (data)
        ObjectNode row2 = mapper.createObjectNode();
        row2.put("type", "tableRow");
        ArrayNode row2Content = mapper.createArrayNode();
        
        // Data cell 1
        ObjectNode dcell1 = mapper.createObjectNode();
        dcell1.put("type", "tableCell");
        ArrayNode dcell1Content = mapper.createArrayNode();
        ObjectNode dcell1Para = mapper.createObjectNode();
        dcell1Para.put("type", "paragraph");
        ArrayNode dcell1ParaContent = mapper.createArrayNode();
        ObjectNode dcell1Text = mapper.createObjectNode();
        dcell1Text.put("type", "text");
        dcell1Text.put("text", "Terminal User");
        dcell1ParaContent.add(dcell1Text);
        dcell1Para.set("content", dcell1ParaContent);
        dcell1Content.add(dcell1Para);
        dcell1.set("content", dcell1Content);
        row2Content.add(dcell1);
        
        // Data cell 2
        ObjectNode dcell2 = mapper.createObjectNode();
        dcell2.put("type", "tableCell");
        ArrayNode dcell2Content = mapper.createArrayNode();
        ObjectNode dcell2Para = mapper.createObjectNode();
        dcell2Para.put("type", "paragraph");
        ArrayNode dcell2ParaContent = mapper.createArrayNode();
        ObjectNode dcell2Text = mapper.createObjectNode();
        dcell2Text.put("type", "text");
        dcell2Text.put("text", "Physical terminal actor");
        dcell2ParaContent.add(dcell2Text);
        dcell2Para.set("content", dcell2ParaContent);
        dcell2Content.add(dcell2Para);
        dcell2.set("content", dcell2Content);
        row2Content.add(dcell2);
        
        // Data cell 3
        ObjectNode dcell3 = mapper.createObjectNode();
        dcell3.put("type", "tableCell");
        ArrayNode dcell3Content = mapper.createArrayNode();
        ObjectNode dcell3Para = mapper.createObjectNode();
        dcell3Para.put("type", "paragraph");
        ArrayNode dcell3ParaContent = mapper.createArrayNode();
        ObjectNode dcell3Text = mapper.createObjectNode();
        dcell3Text.put("type", "text");
        dcell3Text.put("text", "Low-latency operations");
        dcell3ParaContent.add(dcell3Text);
        dcell3Para.set("content", dcell3ParaContent);
        dcell3Content.add(dcell3Para);
        dcell3.set("content", dcell3Content);
        row2Content.add(dcell3);
        
        row2.set("content", row2Content);
        tableContent.add(row2);
        
        table.set("content", tableContent);
        content.add(table);
        
        doc.set("content", content);
        
        String adfJson = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(doc);
        System.out.println("ADF JSON généré avec table native:");
        System.out.println(adfJson);
        
        // Vérifier que c'est valide
        JsonNode parsed = mapper.readTree(adfJson);
        System.out.println("JSON validé: " + (parsed != null));
        
        // Tester la conversion avec Document standard
        Document standardDoc = Document.create();
        standardDoc = standardDoc.h2("Comparaison");
        standardDoc = standardDoc.paragraph("Table ci-dessus générée avec ADF JSON natif");
        
        System.out.println("\nDocument standard ADF:");
        System.out.println(standardDoc.toString());
    }
}