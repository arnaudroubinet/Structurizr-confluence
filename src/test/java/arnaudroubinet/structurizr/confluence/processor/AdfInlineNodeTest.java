package arnaudroubinet.structurizr.confluence.processor;

import com.atlassian.adf.Document;
import com.atlassian.adf.InlineNode;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;

/**
 * Test to explore les nœuds inline et les méthodes de création de liens.
 */
class AdfInlineNodeTest {
    private static final Logger logger = LoggerFactory.getLogger(AdfInlineNodeTest.class);
    
    @Test
    void exploreInlineNodes() {
        logger.info("=== EXPLORATION DES NŒUDS INLINE ADF ===");
        
        Document doc = Document.create();
        
        // Explorer les méthodes statiques de InlineNode
        try {
            Class<?> inlineNodeClass = InlineNode.class;
            Method[] methods = inlineNodeClass.getMethods();
            
            logger.info("Méthodes statiques de InlineNode :");
            for (Method method : methods) {
                if (java.lang.reflect.Modifier.isStatic(method.getModifiers())) {
                    String methodName = method.getName();
                    
                    if (methodName.contains("link") || 
                        methodName.contains("text") || 
                        methodName.contains("create") ||
                        methodName.contains("of") ||
                        methodName.contains("href")) {
                        
                        logger.info("  {} - Paramètres: {}", methodName, java.util.Arrays.toString(method.getParameterTypes()));
                    }
                }
            }
            
            // Essayons de créer des nœuds de texte
            
            try {
                Method textMethod = inlineNodeClass.getMethod("text", String.class);
                Object textNode = textMethod.invoke(null, "Sample text");
                logger.info("✅ InlineNode.text(String) fonctionne : {}", textNode.getClass());
            } catch (Exception e) {
                logger.info("❌ InlineNode.text(String) échoue : {}", e.getMessage());
            }
            
            // Testons d'autres méthodes potentielles
            try {
                Method linkMethod = inlineNodeClass.getMethod("link", String.class, String.class);
                Object linkNode = linkMethod.invoke(null, "arc42", "https://arc42.org/overview");
                logger.info("✅ InlineNode.link(String, String) fonctionne : {}", linkNode.getClass());
            } catch (Exception e) {
                logger.info("❌ InlineNode.link(String, String) échoue : {}", e.getMessage());
            }
            
            // Essayons href
            try {
                Method hrefMethod = inlineNodeClass.getMethod("href", String.class, String.class);
                Object hrefNode = hrefMethod.invoke(null, "arc42", "https://arc42.org/overview");
                logger.info("✅ InlineNode.href(String, String) fonctionne : {}", hrefNode.getClass());
            } catch (Exception e) {
                logger.info("❌ InlineNode.href(String, String) échoue : {}", e.getMessage());
            }
            
        } catch (Exception e) {
            logger.error("Erreur lors de l'exploration: {}", e.getMessage());
        }
    }
}