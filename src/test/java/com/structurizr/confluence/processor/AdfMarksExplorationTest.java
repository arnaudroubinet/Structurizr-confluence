package com.structurizr.confluence.processor;

import com.atlassian.adf.Document;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;

/**
 * Test d'exploration de l'API ADF Builder pour comprendre les marks.
 */
class AdfMarksExplorationTest {
    private static final Logger logger = LoggerFactory.getLogger(AdfMarksExplorationTest.class);
    
    @Test
    void exploreAdfMarksCapabilities() {
        logger.info("=== EXPLORATION DES MARKS ADF ===");
        
        Document doc = Document.create();
        
        // Explorer les méthodes du Document pour voir si on peut créer des marks
        Method[] methods = doc.getClass().getMethods();
        
        logger.info("Méthodes potentielles pour les marks:");
        for (Method method : methods) {
            String methodName = method.getName();
            
            if (methodName.contains("mark") || 
                methodName.contains("strong") || 
                methodName.contains("em") ||
                methodName.contains("code") ||
                methodName.contains("format") ||
                methodName.contains("text")) {
                
                logger.info("  {} - Paramètres: {}", methodName, java.util.Arrays.toString(method.getParameterTypes()));
            }
        }
        
        // Voir s'il y a une façon de créer du texte formaté
        try {
            // Essayons de créer un paragraphe avec du formatage
            
            // Option 1: Voir si paragraph() peut prendre du contenu complexe
            logger.info("\n=== TEST DE CRÉATION DE CONTENU FORMATÉ ===");
            
            // Essayer d'accéder aux classes de nœuds
            logger.info("Classes disponibles:");
            try {
                Class<?> inlineNodeClass = Class.forName("com.atlassian.adf.InlineNode");
                logger.info("✓ InlineNode trouvée");
                
                Method[] inlineMethods = inlineNodeClass.getMethods();
                for (Method method : inlineMethods) {
                    if (java.lang.reflect.Modifier.isStatic(method.getModifiers())) {
                        String methodName = method.getName();
                        if (methodName.contains("text") || methodName.contains("mark") || methodName.contains("strong")) {
                            logger.info("  InlineNode.{} - Paramètres: {}", methodName, java.util.Arrays.toString(method.getParameterTypes()));
                        }
                    }
                }
                
            } catch (ClassNotFoundException e) {
                logger.info("❌ InlineNode non trouvée: {}", e.getMessage());
            }
            
            try {
                Class<?> markClass = Class.forName("com.atlassian.adf.Mark");
                logger.info("✓ Mark trouvée");
                
                Method[] markMethods = markClass.getMethods();
                for (Method method : markMethods) {
                    if (java.lang.reflect.Modifier.isStatic(method.getModifiers())) {
                        logger.info("  Mark.{} - Paramètres: {}", method.getName(), java.util.Arrays.toString(method.getParameterTypes()));
                    }
                }
                
            } catch (ClassNotFoundException e) {
                logger.info("❌ Mark non trouvée: {}", e.getMessage());
            }
            
        } catch (Exception e) {
            logger.error("Erreur lors de l'exploration: {}", e.getMessage());
        }
    }
}