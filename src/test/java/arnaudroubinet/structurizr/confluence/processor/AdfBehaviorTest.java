package arnaudroubinet.structurizr.confluence.processor;

import com.atlassian.adf.Document;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AdfBehaviorTest {
    
    private static final Logger logger = LoggerFactory.getLogger(AdfBehaviorTest.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Test
    public void testAdfDocumentBehavior() throws Exception {
        
        // Test 1: Ajouter plusieurs paragraphes en chaîne
        Document doc = Document.create();
        
        logger.info("Document initial: {}", 
            objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(doc));
        
        // Ajouter le premier paragraphe
        doc = doc.paragraph("Premier paragraphe");
        logger.info("Après premier paragraphe: {}", 
            objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(doc));
        
        // Ajouter le deuxième paragraphe
        doc = doc.paragraph("Deuxième paragraphe");
        logger.info("Après deuxième paragraphe: {}", 
            objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(doc));
        
        // Ajouter le troisième paragraphe
        doc = doc.paragraph("Troisième paragraphe");
        logger.info("Après troisième paragraphe: {}", 
            objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(doc));
        
        // Test 2: Comprendre si l'objet Document est immutable
        Document doc1 = Document.create();
        Document doc2 = doc1.paragraph("Paragraph 1");
        Document doc3 = doc2.paragraph("Paragraph 2");
        
        logger.info("doc1 (original): {}", 
            objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(doc1));
        logger.info("doc2 (+ para 1): {}", 
            objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(doc2));
        logger.info("doc3 (+ para 2): {}", 
            objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(doc3));
    }
}