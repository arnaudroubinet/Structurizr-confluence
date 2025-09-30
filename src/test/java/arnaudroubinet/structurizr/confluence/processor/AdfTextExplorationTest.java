package arnaudroubinet.structurizr.confluence.processor;

import com.atlassian.adf.Document;
import com.atlassian.adf.inline.Text;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;

/**
 * Test d'exploration de l'API Text pour comprendre les marks.
 */
class AdfTextExplorationTest {
    private static final Logger logger = LoggerFactory.getLogger(AdfTextExplorationTest.class);
    
    @Test
    void exploreTextApi() {
        logger.info("=== EXPLORATION DE L'API TEXT ===");
        
        // Explorer les méthodes statiques de Text
        Method[] methods = Text.class.getMethods();
        
        logger.info("Méthodes statiques de Text:");
        for (Method method : methods) {
            if (java.lang.reflect.Modifier.isStatic(method.getModifiers())) {
                logger.info("  Text.{} - Paramètres: {}", method.getName(), java.util.Arrays.toString(method.getParameterTypes()));
            }
        }
        
        logger.info("\nMéthodes d'instance de Text:");
        for (Method method : methods) {
            if (!java.lang.reflect.Modifier.isStatic(method.getModifiers())) {
                String methodName = method.getName();
                if (methodName.contains("mark") || 
                    methodName.contains("strong") || 
                    methodName.contains("em") ||
                    methodName.contains("code") ||
                    methodName.contains("format") ||
                    methodName.contains("style")) {
                    
                    logger.info("  {}.{} - Paramètres: {}", "Text", methodName, java.util.Arrays.toString(method.getParameterTypes()));
                }
            }
        }
        
        // Essayons de créer des nœuds Text
        try {
            
            // Tester Text.of()
            try {
                Method ofMethod = Text.class.getMethod("of", String.class);
                Object textNode = ofMethod.invoke(null, "Test text");
                logger.info("✓ Text.of(String) fonctionne : {}", textNode.getClass());
                
                // Voir quelles méthodes sont disponibles sur l'instance
                Method[] instanceMethods = textNode.getClass().getMethods();
                for (Method method : instanceMethods) {
                    String methodName = method.getName();
                    if (methodName.contains("mark") || 
                        methodName.contains("strong") || 
                        methodName.contains("em") ||
                        methodName.contains("bold") ||
                        methodName.contains("italic") ||
                        methodName.contains("code")) {
                        
                        logger.info("  Instance.{} - Paramètres: {}", methodName, java.util.Arrays.toString(method.getParameterTypes()));
                    }
                }
                
            } catch (Exception e) {
                logger.info("❌ Text.of(String) échoue : {}", e.getMessage());
            }
            
            // Tester d'autres constructeurs possibles
            try {
                Method createMethod = Text.class.getMethod("create", String.class);
                Object textNode = createMethod.invoke(null, "Test text");
                logger.info("✓ Text.create(String) fonctionne : {}", textNode.getClass());
            } catch (Exception e) {
                logger.info("❌ Text.create(String) échoue : {}", e.getMessage());
            }
            
        } catch (Exception e) {
            logger.error("Erreur lors de l'exploration: {}", e.getMessage());
        }
    }
}