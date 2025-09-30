package arnaudroubinet.structurizr.confluence.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Post-processor that transforms ADF documents to inject native table structures
 * where marker comments were placed during initial conversion.
 */
public class AdfTablePostProcessor {
    
    private static final Logger logger = LoggerFactory.getLogger(AdfTablePostProcessor.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    private static final Pattern TABLE_MARKER_PATTERN = 
        Pattern.compile("<!-- ADF_TABLE_START -->(.*?)<!-- ADF_TABLE_END -->", Pattern.DOTALL);
    private static final Pattern GENERIC_NODE_MARKER_PATTERN =
        Pattern.compile("<!-- ADF_NODE_START -->(.*?)<!-- ADF_NODE_END -->", Pattern.DOTALL);
    
    /**
     * Processes an ADF document JSON string and replaces table markers with native table structures.
     */
    public static String postProcessTables(String adfJson) {
        try {
            logger.debug("Starting ADF table post-processing");
            
            JsonNode document = objectMapper.readTree(adfJson);
            JsonNode processedDocument = processNode(document);
            
            String result = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(processedDocument);
            logger.debug("Completed ADF table post-processing");
            return result;
            
        } catch (Exception e) {
            logger.warn("Error during ADF table post-processing, returning original", e);
            return adfJson;
        }
    }
    
    /**
     * Recursively processes a JSON node, looking for table markers in paragraph text.
     */
    private static JsonNode processNode(JsonNode node) {
        if (node.isObject()) {
            ObjectNode objNode = (ObjectNode) node;
            
            // Check if this is a paragraph with table or generic node marker
            if ("paragraph".equals(objNode.path("type").asText())) {
                JsonNode contentArray = objNode.path("content");
                if (contentArray.isArray() && contentArray.size() == 1) {
                    JsonNode textNode = contentArray.get(0);
                    if ("text".equals(textNode.path("type").asText())) {
                        String text = textNode.path("text").asText();
                        
                        // Check if this text contains a table marker
                        Matcher matcher = TABLE_MARKER_PATTERN.matcher(text);
                        if (matcher.find()) {
                            String tableJson = matcher.group(1);
                            try {
                                // Parse the table JSON and return it directly
                                JsonNode tableNode = objectMapper.readTree(tableJson);
                                logger.debug("Replaced table marker with native table structure");
                                return tableNode;
                            } catch (Exception e) {
                                logger.warn("Failed to parse table JSON from marker: " + tableJson, e);
                            }
                        }

                        // Check for generic ADF node marker (e.g., mediaSingle)
                        Matcher genericMatcher = GENERIC_NODE_MARKER_PATTERN.matcher(text);
                        if (genericMatcher.find()) {
                            String nodeJson = genericMatcher.group(1);
                            try {
                                JsonNode adfNode = objectMapper.readTree(nodeJson);
                                logger.debug("Replaced generic ADF node marker with native node");
                                return adfNode;
                            } catch (Exception e) {
                                logger.warn("Failed to parse generic ADF node JSON from marker: " + nodeJson, e);
                            }
                        }
                    }
                }
            }
            
            // Process all child nodes
            ObjectNode result = objectMapper.createObjectNode();
            objNode.fieldNames().forEachRemaining(fieldName -> {
                JsonNode fieldValue = objNode.get(fieldName);
                result.set(fieldName, processNode(fieldValue));
            });
            return result;
            
        } else if (node.isArray()) {
            ArrayNode arrayNode = (ArrayNode) node;
            ArrayNode result = objectMapper.createArrayNode();
            
            for (JsonNode item : arrayNode) {
                result.add(processNode(item));
            }
            return result;
            
        } else {
            // Leaf node, return as-is
            return node;
        }
    }
}