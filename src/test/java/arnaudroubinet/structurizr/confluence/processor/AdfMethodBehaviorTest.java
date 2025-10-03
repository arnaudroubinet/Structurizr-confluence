package arnaudroubinet.structurizr.confluence.processor;

import com.atlassian.adf.Document;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AdfMethodBehaviorTest {

  private static final Logger logger = LoggerFactory.getLogger(AdfMethodBehaviorTest.class);
  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  public void testFromMarkdownVsParagraph() throws Exception {

    // Test 1: Méthode paragraph() - accumulation
    Document doc1 = Document.create();
    doc1 = doc1.paragraph("Premier paragraphe");
    doc1 = doc1.paragraph("Deuxième paragraphe");

    logger.info(
        "Méthode paragraph() - Document final: {}",
        objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(doc1));

    // Test 2: Méthode fromMarkdown() - statique, crée nouveau document
    Document doc2a = Document.create();
    doc2a = doc2a.paragraph("Avant fromMarkdown");
    Document doc2b = Document.fromMarkdown("Contenu fromMarkdown");
    Document doc2c = doc2a.paragraph("Après fromMarkdown");

    logger.info(
        "Document avant fromMarkdown: {}",
        objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(doc2a));
    logger.info(
        "Document créé par fromMarkdown: {}",
        objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(doc2b));
    logger.info(
        "Document final avec paragraph(): {}",
        objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(doc2c));

    // Test 3: fromMarkdown avec plusieurs paragraphes dans le même markdown
    Document doc3 = Document.fromMarkdown("Paragraphe 1\n\nParagraphe 2\n\nParagraphe 3");

    logger.info(
        "fromMarkdown multi-paragraphes - Document: {}",
        objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(doc3));
  }
}
