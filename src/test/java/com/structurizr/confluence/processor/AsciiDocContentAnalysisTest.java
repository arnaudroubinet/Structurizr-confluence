package com.structurizr.confluence.processor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test de débogage pour analyser la conversion du contenu AsciiDoc vers ADF.
 * Permet d'identifier les problèmes de troncature ou de conversion.
 */
public class AsciiDocContentAnalysisTest {
    
    private static final Logger logger = LoggerFactory.getLogger(AsciiDocContentAnalysisTest.class);
    
    private AsciiDocConverter asciiDocConverter;
    private HtmlToAdfConverter htmlToAdfConverter;
    
    @BeforeEach
    void setUp() {
        asciiDocConverter = new AsciiDocConverter();
        htmlToAdfConverter = new HtmlToAdfConverter();
    }
    
    @Test
    void analyzeIntroductionAndGoalsConversion() {
        // Contenu partiel du document 01_introduction_and_goals.adoc
        String asciiDocContent = """
            == Introduction and Goals

            This architecture document follows the https://arc42.org/overview[arc42] template and describes the ITMS (Instant Ticket Manager System) and the platforms with which it interacts. The `itms-workspace.dsl` (Structurizr DSL) is the source of truth for actors, external systems, containers and relationships. This document synchronises prose with that model.

            === Vision

            ITMS enables secure, auditable and resilient management of instant ticket lifecycle operations (validation, locking/unlocking, updates, limit adjustments) across terminal and operator channels while integrating with identity providers, external retail platform, data & audit infrastructures.

            === Stakeholders & Expectations

            [cols="e,4e,4e" options="header"]
            |===
            |Stakeholder |Role / Interest |Key Expectations
            |Terminal User ("Terminal") |Physical or embedded ticket terminal actor accessing ticket services |Low-latency operations, robustness against connectivity issues, clear failure semantics.
            |Operator |Human or automated operator managing configuration, limits, oversight |Strong authentication, traceability, consistent administrative model, zero-trust boundaries.
            |Platform Operations |Operate & monitor ITMS runtime |Predictable scaling, health/metrics endpoints, safe deploy/rollback, minimal blast radius.
            |Security & Compliance |Ensure regulatory & contractual conformity |Immutable audit trail, least privilege, segregated data flows, external identity delegation.
            |External Systems Owners |S3, Data platform, Audit store, Retail system, iDecide, Okta, Postgresql |Stable contracts, backward compatible changes, explicit egress policies.
            |Architecture & Engineering |Evolve system sustainably |Clear container responsibilities, isolation patterns, event-driven integration, change impact clarity.
            |===

            === Goals (Top-Level)

            1. Integrity of ticket state and financial-impacting operations.
            2. High availability for terminal and operator critical flows.
            3. Resilience & graceful degradation when external dependencies fail.
            4. Observability: actionable metrics, tracing context propagation, minimal P99 latency regression.
            5. Security: strong authentication (Okta / Keycloak), explicit ingress/egress enforcement, auditability.

            === Primary Quality Goals

            [cols="e,5e,5e" options="header"]
            |===
            |Quality |Rationale |Implications
            |Integrity |Ticket lifecycle and limit adjustments must be provable and tamper‑evident |Use Postgresql as system-of-record; append-only audit streams; Debezium capture.
            |Availability |Continuous terminal & operator service drives business value |Multi‑AZ deployments, anti-affinity, horizontal scaling, health-based routing.
            |Resilience |External systems (S3, audit, data, retail) may degrade |Bulkhead via ingress/egress separation; timeout & retry policies; circuit breaking.
            |Security |Sensitive operations & identities |Delegated OAuth/OIDC (Okta/Keycloak), token validation at ingress, principle of least privilege.
            |Observability |Fast incident triage |Structured access logs (no payload), OpenTelemetry headers injection, Kafka event tracing IDs.
            |Evolvability |Model will grow (new adapters/services) |Container boundary clarity; stable contracts; DSL as canonical model.
            |===

            === Alignment with Previous Platform Docs
            Legacy references to a broader "ITF" platform are superseded here by the focused ITMS moteur & retail interaction model as represented in the DSL.
            """;
        
        logger.info("=== ANALYSE DE LA CONVERSION ASCIIDOC ===");
        logger.info("Taille du contenu AsciiDoc original: {} caractères", asciiDocContent.length());
        
        // Étape 1: Conversion AsciiDoc vers HTML
        String htmlContent = asciiDocConverter.convertToHtml(asciiDocContent, "01_introduction_and_goals");
        logger.info("Taille du contenu HTML converti: {} caractères", htmlContent.length());
        logger.info("Contenu HTML (début):\\n{}", htmlContent.substring(0, Math.min(500, htmlContent.length())));
        
        // Vérifier s'il y a des erreurs dans la conversion AsciiDoc
        if (htmlContent.contains("asciidoc-error")) {
            logger.error("ERREUR DÉTECTÉE dans la conversion AsciiDoc!");
            logger.error("Contenu HTML complet:\\n{}", htmlContent);
            return;
        }
        
        // Étape 2: Conversion HTML vers ADF
        String adfJson = htmlToAdfConverter.convertToAdfJson(htmlContent, "01_introduction_and_goals");
        logger.info("Taille du JSON ADF: {} caractères", adfJson.length());
        
        // Analyser le contenu ADF
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode adfTree = mapper.readTree(adfJson);
            
            logger.info("Structure ADF:");
            logger.info("- Version: {}", adfTree.get("version"));
            logger.info("- Type: {}", adfTree.get("type"));
            
            com.fasterxml.jackson.databind.JsonNode content = adfTree.get("content");
            if (content != null && content.isArray()) {
                logger.info("- Nombre d'éléments de contenu: {}", content.size());
                
                for (int i = 0; i < content.size(); i++) {
                    com.fasterxml.jackson.databind.JsonNode element = content.get(i);
                    String type = element.get("type").asText();
                    logger.info("  [{}] Type: {}", i, type);
                    
                    if ("paragraph".equals(type) || type.startsWith("heading")) {
                        com.fasterxml.jackson.databind.JsonNode elemContent = element.get("content");
                        if (elemContent != null && elemContent.isArray() && elemContent.size() > 0) {
                            com.fasterxml.jackson.databind.JsonNode firstText = elemContent.get(0);
                            if (firstText.has("text")) {
                                String text = firstText.get("text").asText();
                                String preview = text.length() > 100 ? text.substring(0, 100) + "..." : text;
                                logger.info("       Texte: {}", preview);
                            }
                        }
                    }
                }
            }
            
            logger.info("ADF JSON complet:\\n{}", mapper.writerWithDefaultPrettyPrinter().writeValueAsString(adfTree));
            
        } catch (Exception e) {
            logger.error("Erreur lors de l'analyse de l'ADF: {}", e.getMessage(), e);
        }
    }
}