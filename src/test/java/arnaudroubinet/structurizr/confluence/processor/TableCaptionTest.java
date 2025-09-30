package arnaudroubinet.structurizr.confluence.processor;

import com.atlassian.adf.Document;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tests to verify le support des captions de tableaux.
 */
public class TableCaptionTest {
    private static final Logger logger = LoggerFactory.getLogger(TableCaptionTest.class);
    private HtmlToAdfConverter converter;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    public void setUp() {
        converter = new HtmlToAdfConverter();
    }

    @Test
    public void testCurrentTableCaptionHandling() throws JsonProcessingException {
        
        String htmlContent = "<table><caption>Titre du tableau de test</caption><tr><th>Header 1</th><th>Header 2</th></tr><tr><td>Data 1</td><td>Data 2</td></tr></table>";
        
        Document adfDocument = converter.convertToAdf(htmlContent, "Test Table Caption");
        String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(adfDocument);
        
        logger.info("HTML d'entrée: {}", htmlContent);
        logger.info("ADF généré: {}", json);
        
        // Analyser si la caption est traitée
        boolean hasTableType = json.contains("\"type\" : \"table\"");
        boolean hasCaptionText = json.contains("Titre du tableau");
        
        logger.info("Tableau détecté: {}", hasTableType);
        logger.info("Texte de caption trouvé: {}", hasCaptionText);
        
        // Si la caption n'est pas trouvée, elle est probablement ignorée
        if (hasTableType && !hasCaptionText) {
            logger.warn("CAPTION IGNORÉE : Le <caption> du tableau n'apparaît pas dans l'ADF généré");
        }
    }

    @Test
    public void testTableWithoutCaption() throws JsonProcessingException {
        
        String htmlContent = "<table><tr><th>Header 1</th><th>Header 2</th></tr><tr><td>Data 1</td><td>Data 2</td></tr></table>";
        
        Document adfDocument = converter.convertToAdf(htmlContent, "Test Table No Caption");
        String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(adfDocument);
        
        logger.info("HTML d'entrée: {}", htmlContent);
        logger.info("ADF généré: {}", json);
    }
    
    @Test
    public void testMultipleCaptionElements() throws JsonProcessingException {
        
        // Test avec caption avant et après le tableau (edge case)
        String htmlContent1 = "<table><caption>Caption avant</caption><tr><th>Header</th></tr><tr><td>Data</td></tr></table>";
        String htmlContent2 = "<div><p>Titre: Important Table</p><table><tr><th>Header</th></tr><tr><td>Data</td></tr></table></div>";
        
        Document adf1 = converter.convertToAdf(htmlContent1, "Test Caption Before");
        Document adf2 = converter.convertToAdf(htmlContent2, "Test Manual Caption");
        
        String json1 = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(adf1);
        String json2 = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(adf2);
        
        logger.info("HTML avec <caption>: {}", htmlContent1);
        logger.info("ADF avec <caption>: {}", json1);
        logger.info("HTML avec titre manuel: {}", htmlContent2);
        logger.info("ADF avec titre manuel: {}", json2);
        
        // Comparer les approches
        boolean captionInJson1 = json1.contains("Caption avant");
        boolean titleInJson2 = json2.contains("Important Table");
        
        logger.info("Caption HTML trouvée dans ADF: {}", captionInJson1);
        logger.info("Titre manuel trouvé dans ADF: {}", titleInJson2);
    }
}