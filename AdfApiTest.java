import com.atlassian.adf.Document;

public class AdfApiTest {
    public static void main(String[] args) {
        Document doc = Document.create();
        
        // Afficher toutes les méthodes publiques disponibles
        System.out.println("Méthodes disponibles sur Document:");
        for (java.lang.reflect.Method method : doc.getClass().getMethods()) {
            if (method.getDeclaringClass() == doc.getClass() && 
                method.getParameterCount() <= 2 &&
                method.getName().length() < 15) {
                System.out.println("  " + method.getName() + "(" + method.getParameterCount() + " params)");
            }
        }
    }
}