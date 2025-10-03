package arnaudroubinet.structurizr.confluence.processor;

import com.atlassian.adf.Document;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Tests to verify le support des listes ordonnées dans ADF Builder. */
public class OrderedListTest {
  private static final Logger logger = LoggerFactory.getLogger(OrderedListTest.class);
  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  public void testOrderedListSupport() throws JsonProcessingException {

    // Créer un document simple avec un paragraphe pour commencer
    Document document = Document.create().paragraph("Test document");

    String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(document);
    logger.info("Document JSON: {}", json);

    // Maintenant tester si orderedList ou numberedList existent en regardant les méthodes
    logger.info("Méthodes disponibles dans Document.class:");
    for (java.lang.reflect.Method method : Document.class.getDeclaredMethods()) {
      String methodName = method.getName().toLowerCase();
      if (methodName.contains("list")
          || methodName.contains("ordered")
          || methodName.contains("numbered")
          || methodName.contains("blockquote")
          || methodName.contains("quote")) {
        logger.info("- {}", method.getName());
      }
    }

    // Test si bulletList fonctionne (pour comparaison)
    Document bulletDocument =
        Document.create()
            .bulletList(
                list -> {
                  list.item(item -> item.paragraph("Item 1"));
                  list.item(item -> item.paragraph("Item 2"));
                });

    String bulletJson =
        objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(bulletDocument);
    logger.info("BulletList JSON: {}", bulletJson);

    // TEST ORDERED LIST !!!
    Document orderedDocument =
        Document.create()
            .orderedList(
                list -> {
                  list.item(item -> item.paragraph("Premier item"));
                  list.item(item -> item.paragraph("Deuxième item"));
                  list.item(item -> item.paragraph("Troisième item"));
                });

    String orderedJson =
        objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(orderedDocument);
    logger.info("OrderedList JSON: {}", orderedJson);

    // TEST BLOCKQUOTE !!!
    Document quoteDocument =
        Document.create()
            .quote(
                quote -> {
                  quote.paragraph("Ceci est une citation importante pour illustrer un point.");
                });

    String quoteJson =
        objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(quoteDocument);
    logger.info("Quote JSON: {}", quoteJson);
  }
}
