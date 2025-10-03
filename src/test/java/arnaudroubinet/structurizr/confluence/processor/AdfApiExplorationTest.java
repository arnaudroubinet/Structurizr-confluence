package arnaudroubinet.structurizr.confluence.processor;

import com.atlassian.adf.Document;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Test to explore l'API ADF Builder et trouver les méthodes de liens disponibles. */
class AdfApiExplorationTest {
  private static final Logger logger = LoggerFactory.getLogger(AdfApiExplorationTest.class);

  @Test
  void exploreAdfBuilderMethods() {
    logger.info("=== EXPLORATION DE L'API ADF BUILDER ===");

    Document doc = Document.create();

    // Obtenir toutes les méthodes publiques de la classe Document
    Method[] methods = doc.getClass().getMethods();

    logger.info("Méthodes disponibles dans Document :");
    for (Method method : methods) {
      String methodName = method.getName();

      // Filtrer pour les méthodes qui pourraient être liées aux liens
      if (methodName.contains("link")
          || methodName.contains("href")
          || methodName.contains("url")
          || methodName.contains("anchor")
          || methodName.contains("text")
          || methodName.equals("paragraph")
          || methodName.equals("p")) {

        logger.info(
            "  {} - Paramètres: {}",
            methodName,
            java.util.Arrays.toString(method.getParameterTypes()));
      }
    }

    // Tester si certaines méthodes existent
    try {
      // Test des méthodes courantes

      // Voir si on peut créer un paragraphe simple
      Document testDoc = Document.create();
      testDoc = testDoc.paragraph("Test simple");
      logger.info("✅ paragraph(String) fonctionne");

      // Tester d'autres méthodes potentielles pour les liens
      // (Ces tests échoueront probablement mais nous donneront des infos)
      try {
        // Supposons que link() pourrait exister
        Method linkMethod = doc.getClass().getMethod("link", String.class, String.class);
        logger.info("✅ link(String, String) existe : {}", linkMethod);
      } catch (NoSuchMethodException e) {
        logger.info("❌ link(String, String) n'existe pas");
      }

    } catch (Exception e) {
      logger.error("Erreur lors des tests: {}", e.getMessage());
    }
  }
}
