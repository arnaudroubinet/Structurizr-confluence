package arnaudroubinet.structurizr.confluence;

import com.atlassian.adf.Document;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.structurizr.Workspace;
import arnaudroubinet.structurizr.confluence.processor.AsciiDocConverter;
import arnaudroubinet.structurizr.confluence.processor.HtmlToAdfConverter;
import com.structurizr.documentation.Section;
import com.structurizr.util.WorkspaceUtils;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test des méthodes de production utilisées par l'exporteur principal
 */
public class ProductionMethodTest {

    private static final Logger logger = LoggerFactory.getLogger(ProductionMethodTest.class);
    
    private final AsciiDocConverter asciiDocConverter = new AsciiDocConverter();
    private final HtmlToAdfConverter htmlToAdfConverter = new HtmlToAdfConverter();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void testProductionConvertToAdf() throws Exception {
        logger.info("=== Test de la méthode convertToAdf() utilisée par l'exporteur ===");
        
        // Charger le workspace
        File workspaceFile = new File("demo/itms-workspace.json");
        Workspace workspace = WorkspaceUtils.loadWorkspaceFromJson(workspaceFile);
        
        // Prendre la première section
        Collection<Section> sections = workspace.getDocumentation().getSections();
        Section firstSection = sections.iterator().next();
        String originalContent = firstSection.getContent();
        
        // Convertir AsciiDoc vers HTML
        String htmlContent = asciiDocConverter.convertToHtml(originalContent, firstSection.getTitle(), "1", "main");
        
        // Utiliser la méthode convertToAdf() comme l'exporteur principal
        String sectionTitle = firstSection.getTitle() != null ? firstSection.getTitle() : firstSection.getFilename();
        Document adfDocument = htmlToAdfConverter.convertToAdf(htmlContent, sectionTitle);
        
        // Convertir en JSON pour vérifier le contenu
        String adfJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(adfDocument);
        
        logger.info("=== RÉSULTAT DE LA MÉTHODE DE PRODUCTION ===");
        logger.info("Taille: {} caractères JSON", adfJson.length());
        logger.info("Contient 'type\" : \"table\"': {}", adfJson.contains("\"type\" : \"table\""));
        logger.info("Contient des marqueurs: {}", adfJson.contains("<!-- ADF_TABLE_START -->"));
        
        // Vérifications
        assertNotNull(adfDocument, "Le document ADF ne doit pas être null");
        assertTrue(adfJson.length() > 10, "Le document ADF doit contenir du contenu significatif");
        
        // Le test principal : vérifier que les tables natives sont générées
        if (adfJson.contains("\"type\" : \"table\"")) {
            logger.info("✅ SUCCÈS : Tables natives ADF détectées !");
            assertFalse(adfJson.contains("<!-- ADF_TABLE_START -->"), 
                       "Le JSON ADF ne doit plus contenir de marqueurs de table");
        } else {
            logger.warn("⚠️  Tables natives non détectées - limitation de l'ADF Builder");
            // Afficher un échantillon pour déboguer
            logger.info("Échantillon JSON (500 premiers caractères):\n{}", 
                       adfJson.length() > 500 ? adfJson.substring(0, 500) + "..." : adfJson);
        }
        
        logger.info("=== Fin du test de la méthode de production ===");
    }
}