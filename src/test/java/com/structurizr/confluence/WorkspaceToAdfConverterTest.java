package com.structurizr.confluence;

import com.atlassian.adf.Document;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.structurizr.Workspace;
import com.structurizr.confluence.processor.AsciiDocConverter;
import com.structurizr.confluence.processor.HtmlToAdfConverter;
import com.structurizr.documentation.Section;
import com.structurizr.util.WorkspaceUtils;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Path;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test qui lit un workspace Structurizr et le convertit au format Confluence (ADF) 
 * sans l'envoyer vers Confluence. Affiche le contenu ADF généré dans les logs.
 */
public class WorkspaceToAdfConverterTest {
    
    private static final Logger logger = LoggerFactory.getLogger(WorkspaceToAdfConverterTest.class);
    
    private final AsciiDocConverter asciiDocConverter = new AsciiDocConverter();
    private final HtmlToAdfConverter htmlToAdfConverter = new HtmlToAdfConverter();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void convertWorkspaceToAdfFormat() throws Exception {
        logger.info("=== Début de la conversion workspace vers ADF ===");
        
        // Charger le workspace depuis le fichier JSON
        File workspaceFile = Path.of("demo/itms-workspace.json").toFile();
        assertTrue(workspaceFile.exists(), "Le fichier workspace demo/itms-workspace.json doit exister");
        
        Workspace workspace = WorkspaceUtils.loadWorkspaceFromJson(workspaceFile);
        assertNotNull(workspace, "Le workspace ne doit pas être null");
        
        logger.info("Workspace chargé: '{}' - Description: '{}'", 
                workspace.getName(), workspace.getDescription());
        
        // Convertir la première section de documentation du workspace
        if (workspace.getDocumentation() != null && workspace.getDocumentation().getSections() != null) {
            convertDocumentationSections(workspace);
        } else {
            logger.info("Aucune section de documentation trouvée dans le workspace");
        }
        
        // Note: Les ADRs sont ignorés pour ce test - on ne traite que la première section de documentation
        
        logger.info("=== Fin de la conversion workspace vers ADF ===");
    }
    
    /**
     * Convertit la première section de documentation du workspace en ADF
     */
    private void convertDocumentationSections(Workspace workspace) throws Exception {
        Collection<Section> sections = workspace.getDocumentation().getSections();
        logger.info("Conversion de la première section sur {} sections de documentation disponibles", sections.size());
        
        // Prendre seulement la première section
        Section firstSection = sections.iterator().next();
        
        logger.info("--- Traitement de la première section: '{}' (format: {}) ---", 
                firstSection.getTitle() != null ? firstSection.getTitle() : firstSection.getFilename(), 
                firstSection.getFormat());
        
        String originalContent = firstSection.getContent();
        if (originalContent == null || originalContent.trim().isEmpty()) {
            logger.warn("Section vide ignorée: {}", firstSection.getFilename());
            return;
        }
        
        // Afficher le contenu original (pré-modification)
        logger.info("=== CONTENU ORIGINAL (PRÉ-MODIFICATION) ===");
        logger.info("Titre: {}", firstSection.getTitle() != null ? firstSection.getTitle() : "Sans titre");
        logger.info("Format: {}", firstSection.getFormat());
        logger.info("Taille: {} caractères", originalContent.length());
        logger.info("Contenu AsciiDoc original:\n{}", originalContent);
        
        // Convertir AsciiDoc vers HTML puis vers ADF
        String htmlContent = asciiDocConverter.convertToHtml(originalContent, firstSection.getTitle(), "1", "main");
        logger.debug("HTML généré ({} caractères): {}", htmlContent.length(), 
                htmlContent.length() > 500 ? htmlContent.substring(0, 500) + "..." : htmlContent);
        
        // Analyser la structure HTML pour déboguer
        org.jsoup.nodes.Document htmlDoc = org.jsoup.Jsoup.parse(htmlContent);
        logger.info("Structure HTML analysée:");
        logger.info("  - Nombre d'éléments h1: {}", htmlDoc.select("h1").size());
        logger.info("  - Nombre d'éléments h2: {}", htmlDoc.select("h2").size());
        logger.info("  - Nombre d'éléments h3: {}", htmlDoc.select("h3").size());
        logger.info("  - Nombre d'éléments p: {}", htmlDoc.select("p").size());
        logger.info("  - Nombre d'éléments ul: {}", htmlDoc.select("ul").size());
        logger.info("  - Nombre d'éléments table: {}", htmlDoc.select("table").size());
        
        // Convertir HTML vers ADF avec support natif des tables
        String sectionTitle = firstSection.getTitle() != null ? firstSection.getTitle() : firstSection.getFilename();
        String adfJson = htmlToAdfConverter.convertToAdfJson(htmlContent, sectionTitle);
        assertNotNull(adfJson, "Le JSON ADF ne doit pas être null");
        
        // Afficher le JSON ADF traité (avec tables natives)
        logger.info("=== CONTENU TRANSFORMÉ (POST-MODIFICATION) ===");
        logger.info("Format de sortie: ADF (Atlassian Document Format)");
        logger.info("Taille: {} caractères JSON", adfJson.length());
        logger.info("Document ADF généré avec tables natives:\n{}", adfJson);
        
        // Créer un Document ADF à partir du JSON pour les vérifications de base seulement
        JsonNode adfNode = objectMapper.readTree(adfJson);
        Document adfDocument = objectMapper.treeToValue(adfNode, Document.class);
        
        // Vérifications de base
        assertNotNull(adfDocument, "Le document ADF ne doit pas être null");
        assertTrue(adfJson.length() > 10, "Le document ADF doit contenir du contenu significatif");
        
        // Vérifier que les tables natives sont présentes dans le JSON
        assertTrue(adfJson.contains("\"type\" : \"table\""), "Le JSON ADF doit contenir des tables natives");
        assertFalse(adfJson.contains("<!-- ADF_TABLE_START -->"), "Le JSON ADF ne doit plus contenir de marqueurs de table");
        
        logger.info("=== RÉSUMÉ DE LA TRANSFORMATION ===");
        logger.info("AsciiDoc ({} caractères) → HTML ({} caractères) → ADF JSON ({} caractères)", 
                originalContent.length(), htmlContent.length(), adfJson.length());
    }
}