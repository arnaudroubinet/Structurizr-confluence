package com.structurizr.confluence;

import com.structurizr.confluence.processor.HtmlToAdfConverter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test pour vérifier que les méthodes de production génèrent bien des tables natives ADF.
 */
public class TableVerificationTest {

    @Test
    public void testProductionMethodsGenerateNativeTables() throws Exception {
        // Test 1: Avec contenu HTML direct contenant un tableau
        String htmlContent = """
            <h2>Test Section avec Tableau</h2>
            <p>Ce contenu contient un tableau :</p>
            <table>
                <thead>
                    <tr>
                        <th>Nom</th>
                        <th>Description</th>
                        <th>Status</th>
                    </tr>
                </thead>
                <tbody>
                    <tr>
                        <td>Service A</td>
                        <td>Service principal</td>
                        <td>Actif</td>
                    </tr>
                    <tr>
                        <td>Service B</td>
                        <td>Service secondaire</td>
                        <td>Inactif</td>
                    </tr>
                </tbody>
            </table>
            <p>Fin du tableau.</p>
            """;

        System.out.println("=== Test avec contenu HTML direct ===");

        // Convertir avec la méthode de production convertToAdfJson()
        HtmlToAdfConverter converter = new HtmlToAdfConverter();
        String adfJson = converter.convertToAdfJson(htmlContent, "Html");

        System.out.println("Contenu JSON généré (1000 premiers caractères): " + adfJson.substring(0, Math.min(1000, adfJson.length())) + "...");

        // Vérifier que le JSON contient des structures de table natives (avec espaces flexibles)
        assertTrue(adfJson.contains("\"type\" : \"table\"") || adfJson.contains("\"type\":\"table\""), 
                "Le JSON ADF devrait contenir des tables natives (type: table)");
        assertTrue(adfJson.contains("\"type\" : \"tableRow\"") || adfJson.contains("\"type\":\"tableRow\""), 
                "Le JSON ADF devrait contenir des lignes de table natives (type: tableRow)");
        assertTrue(adfJson.contains("\"type\" : \"tableHeader\"") || adfJson.contains("\"type\":\"tableHeader\"") ||
                   adfJson.contains("\"type\" : \"tableCell\"") || adfJson.contains("\"type\":\"tableCell\""), 
                "Le JSON ADF devrait contenir des cellules de table natives");

        // Vérifier qu'il ne contient pas de texte pipe-separated
        assertFalse(adfJson.contains("| "), 
                "Le JSON ADF ne devrait pas contenir de tables au format texte pipe-separated");

        System.out.println("✅ Les méthodes de production génèrent bien des tables natives ADF");
        System.out.println("Taille du JSON ADF: " + adfJson.length() + " caractères");
        
        // Compter les occurrences de tables (avec espaces flexibles)
        long tableCount = adfJson.split("\"type\" : \"table\"").length - 1;
        if (tableCount == 0) {
            tableCount = adfJson.split("\"type\":\"table\"").length - 1;
        }
        System.out.println("Nombre de tables natives détectées: " + tableCount);
    }
}