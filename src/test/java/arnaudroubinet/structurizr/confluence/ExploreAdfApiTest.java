package arnaudroubinet.structurizr.confluence;

import com.atlassian.adf.Document;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;

/** Test to explore l'API Document d'ADF */
public class ExploreAdfApiTest {

  @Test
  public void exploreDocumentMethods() {
    Document doc = Document.create();

    System.out.println("=== Méthodes disponibles sur Document ===");
    Method[] methods = doc.getClass().getMethods();

    for (Method method : methods) {
      // Afficher seulement les méthodes non-héritées d'Object et publiques
      if (!method.getDeclaringClass().equals(Object.class)
          && method.getName().length() < 20) { // Filtrer les noms très longs
        System.out.println(method.getName() + " : " + method.getReturnType().getSimpleName());
      }
    }

    System.out.println("\n=== Test des méthodes paragraph et liens ===");

    // Test 1: paragraph simple
    try {
      doc = doc.paragraph("Test simple");
      System.out.println("✓ paragraph(String) fonctionne");
    } catch (Exception e) {
      System.out.println("✗ paragraph(String) échoue: " + e.getMessage());
    }

    // Test 2: fromMarkdown pour les liens
    try {
      Document docWithLink =
          Document.create()
              .fromMarkdown("[Documentation officielle](https://developer.atlassian.com)");
      System.out.println("✓ fromMarkdown avec lien fonctionne");
      System.out.println("Document avec lien: " + docWithLink.toString());

      // Test si on peut extraire le contenu d'un paragraphe
      System.out.println("Children: " + docWithLink.children().size());
      if (docWithLink.children().size() > 0) {
        System.out.println(
            "Premier enfant: " + docWithLink.children().get(0).getClass().getSimpleName());
      }
    } catch (Exception e) {
      System.out.println("✗ fromMarkdown échoue: " + e.getMessage());
    }

    // Test 3: paragraph avec contenu plus complexe
    try {
      doc =
          doc.paragraph(
              p -> {
                // Explorer l'API du builder de paragraphe
                System.out.println("✓ paragraph(lambda) accessible");
              });
      System.out.println("✓ paragraph(lambda) fonctionne");
    } catch (Exception e) {
      System.out.println("✗ paragraph(lambda) échoue: " + e.getMessage());
    }

    System.out.println("\nDocument created successfully");
  }
}
