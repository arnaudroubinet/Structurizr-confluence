package com.structurizr.confluence.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Post-processes ADF JSON to center-align supported nodes across the document.
 * NOTE: Confluence ADF ingestion does not accept block-level marks on paragraph/heading.
 * We therefore only center media using mediaSingle.attrs.layout = "center".
 */
public final class AdfAlignmentPostProcessor {

    private static final Logger logger = LoggerFactory.getLogger(AdfAlignmentPostProcessor.class);

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private AdfAlignmentPostProcessor() {}

    public static String centerAlignAll(String adfJson) {
        if (adfJson == null || adfJson.isBlank()) return adfJson;
        try {
            JsonNode root = objectMapper.readTree(adfJson);
            if (root != null && root.isObject()) {
                centerAlignNode((ObjectNode) root);
            }
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
        } catch (Exception e) {
            logger.warn("ADF alignment post-processing failed, returning original JSON", e);
            return adfJson;
        }
    }

    private static void centerAlignNode(ObjectNode node) {
        if (node == null) return;

        // If this node has a type, apply alignment rules where applicable
        JsonNode typeNode = node.get("type");
        if (typeNode != null && typeNode.isTextual()) {
            String type = typeNode.asText();

            switch (type) {
                case "mediaSingle":
                    ensureMediaSingleCentered(node);
                    break;
                default:
                    // no-op
            }
        }

        // Recurse into content arrays
        JsonNode contentNode = node.get("content");
        if (contentNode != null && contentNode.isArray()) {
            ArrayNode content = (ArrayNode) contentNode;
            for (JsonNode child : content) {
                if (child.isObject()) {
                    centerAlignNode((ObjectNode) child);
                }
            }
        }
    }

    private static void ensureMediaSingleCentered(ObjectNode mediaSingle) {
        ObjectNode attrs = mediaSingle.has("attrs") && mediaSingle.get("attrs").isObject()
                ? (ObjectNode) mediaSingle.get("attrs")
                : mediaSingle.putObject("attrs");
        // Force center layout for mediaSingle
        attrs.put("layout", "center");
    }
}
