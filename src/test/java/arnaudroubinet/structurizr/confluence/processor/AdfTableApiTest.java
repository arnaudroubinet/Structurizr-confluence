package arnaudroubinet.structurizr.confluence.processor;

import com.atlassian.adf.Document;
import java.lang.reflect.Method;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

/** Test to explore l'API ADF disponible pour les tables. */
public class AdfTableApiTest {

  @Test
  public void exploreTableApi() {
    System.out.println("=== Exploring ADF Document API ===");

    // Explorer les méthodes du Document
    Method[] methods = Document.class.getMethods();

    System.out.println("\nMéthodes contenant 'table':");
    Arrays.stream(methods)
        .filter(m -> m.getName().toLowerCase().contains("table"))
        .forEach(
            m ->
                System.out.println(
                    "  "
                        + m.getName()
                        + "("
                        + Arrays.toString(m.getParameterTypes())
                        + ") -> "
                        + m.getReturnType()));

    System.out.println("\nToutes les méthodes publiques du Document:");
    Arrays.stream(methods)
        .filter(m -> m.getDeclaringClass() == Document.class)
        .forEach(
            m ->
                System.out.println(
                    "  "
                        + m.getName()
                        + "("
                        + Arrays.toString(m.getParameterTypes())
                        + ") -> "
                        + m.getReturnType()));

    // Test simple de création de document
    Document doc = Document.create();
    System.out.println("\nDocument created: " + doc.getClass().getName());

    // Essayer de voir si on peut accéder à des méthodes de table
    try {
      // Tenter d'utiliser une méthode table si elle existe
      Method tableMethod = Document.class.getMethod("table");
      System.out.println("Méthode table() trouvée: " + tableMethod);
    } catch (NoSuchMethodException e) {
      System.out.println("Pas de méthode table() simple");
    }

    try {
      // Chercher des méthodes avec des paramètres
      Method[] tableMethods = Document.class.getMethods();
      for (Method m : tableMethods) {
        if (m.getName().equals("table")) {
          System.out.println("Méthode table trouvée: " + m);
        }
      }
    } catch (Exception e) {
      System.out.println("Erreur lors de la recherche: " + e.getMessage());
    }
  }

  @Test
  public void testDocumentChaining() {
    System.out.println("=== Test Document Chaining ===");

    Document doc = Document.create();

    // Test des méthodes de base connues
    doc = doc.h1("Test Title");
    doc = doc.paragraph("Test paragraph");

    // Essayer de créer une liste avec une API similaire à bulletList
    doc =
        doc.bulletList(
            list -> {
              list.item(item -> item.paragraph("Item 1"));
              list.item(item -> item.paragraph("Item 2"));
            });

    System.out.println("Document avec contenu créé: " + doc.toString().length() + " caractères");
  }
}
