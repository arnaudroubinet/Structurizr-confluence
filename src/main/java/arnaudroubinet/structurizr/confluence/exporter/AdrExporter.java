package arnaudroubinet.structurizr.confluence.exporter;

import arnaudroubinet.structurizr.confluence.client.ConfluenceClient;
import arnaudroubinet.structurizr.confluence.processor.AsciiDocConverter;
import arnaudroubinet.structurizr.confluence.processor.HtmlToAdfConverter;
import arnaudroubinet.structurizr.confluence.processor.MarkdownConverter;
import com.atlassian.adf.Document;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.structurizr.Workspace;
import com.structurizr.documentation.Decision;
import java.util.Collection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles export of Architecture Decision Records (ADRs) to Confluence. Separates ADR-specific
 * export logic from main export orchestration.
 */
public class AdrExporter {

  private static final Logger logger = LoggerFactory.getLogger(AdrExporter.class);

  private static final String FORMAT_ASCIIDOC = "AsciiDoc";
  private static final String FORMAT_ASCIIDOC_LOWER = "asciidoc";
  private static final String FORMAT_MARKDOWN = "Markdown";
  private static final String FORMAT_MARKDOWN_SHORT = "md";

  private final ConfluenceClient confluenceClient;
  private final ObjectMapper objectMapper;
  private final HtmlToAdfConverter htmlToAdfConverter;
  private final AsciiDocConverter asciiDocConverter;
  private final MarkdownConverter markdownConverter;

  public AdrExporter(
      ConfluenceClient confluenceClient,
      ObjectMapper objectMapper,
      HtmlToAdfConverter htmlToAdfConverter,
      AsciiDocConverter asciiDocConverter,
      MarkdownConverter markdownConverter) {
    this.confluenceClient = confluenceClient;
    this.objectMapper = objectMapper;
    this.htmlToAdfConverter = htmlToAdfConverter;
    this.asciiDocConverter = asciiDocConverter;
    this.markdownConverter = markdownConverter;
  }

  /**
   * Exports all architecture decision records from a workspace to Confluence.
   *
   * @param workspace the workspace containing ADRs
   * @param parentPageId the parent page ID where ADRs will be created
   * @param branchName the branch name for context
   * @throws Exception if export fails
   */
  public void exportDecisions(Workspace workspace, String parentPageId, String branchName)
      throws Exception {
    if (workspace.getDocumentation() == null
        || workspace.getDocumentation().getDecisions().isEmpty()) {
      logger.info("No architecture decision records found in workspace");
      return;
    }

    Collection<Decision> decisions = workspace.getDocumentation().getDecisions();
    logger.info("Exporting {} architecture decision records", decisions.size());

    // Create main ADR page
    Document adrMainDoc =
        Document.create()
            .paragraph("This page contains all architecture decision records for this project.");

    String adrMainPageTitle = "Architecture Decision Records - " + branchName;
    String adrMainPageId =
        confluenceClient.createOrUpdatePage(
            adrMainPageTitle, convertDocumentToJson(adrMainDoc), parentPageId);
    logger.info("Created/updated main ADR page with ID: {}", adrMainPageId);

    // Create individual ADR pages
    for (Decision decision : decisions) {
      exportDecision(decision, adrMainPageId, workspace, branchName);
    }
  }

  /**
   * Exports a single architecture decision record to Confluence.
   *
   * @param decision the decision to export
   * @param parentPageId the parent page ID
   * @param workspace the workspace for context
   * @param branchName the branch name
   * @throws Exception if export fails
   */
  private void exportDecision(
      Decision decision, String parentPageId, Workspace workspace, String branchName)
      throws Exception {
    Document decisionDoc = Document.create();

    // Add decision metadata
    decisionDoc.h2("Decision Information");
    decisionDoc.bulletList(
        list -> {
          list.item("ID: " + decision.getId());
          list.item("Title: " + decision.getTitle());

          if (decision.getStatus() != null && !decision.getStatus().trim().isEmpty()) {
            list.item("Status: " + decision.getStatus());
          }

          if (decision.getDate() != null) {
            list.item("Date: " + decision.getDate().toString());
          }
        });

    // Add decision content (convert from AsciiDoc/Markdown if needed)
    if (decision.getContent() != null && !decision.getContent().trim().isEmpty()) {
      decisionDoc.h2("Content");

      String formatName = decision.getFormat() != null ? decision.getFormat().name() : "";
      String htmlContent;

      if (isAsciiDocFormat(formatName)) {
        logger.debug("Converting AsciiDoc content for ADR: {}", decision.getTitle());
        String workspaceId = getWorkspaceId(workspace);
        htmlContent =
            asciiDocConverter.convertToHtml(
                decision.getContent(), "ADR " + decision.getId(), workspaceId, branchName);
      } else if (isMarkdownFormat(formatName)) {
        logger.debug("Converting Markdown content for ADR: {}", decision.getTitle());
        htmlContent = markdownConverter.toHtml(decision.getContent());
      } else {
        logger.debug(
            "Treating content as HTML for ADR: {} (format: {})", decision.getTitle(), formatName);
        htmlContent = decision.getContent();
      }

      // Convert HTML content to structured ADF
      Document convertedContent = htmlToAdfConverter.convertToAdf(htmlContent, "Content");
      decisionDoc = combineDocuments(decisionDoc, convertedContent);
    }

    // Add links to other decisions
    if (!decision.getLinks().isEmpty()) {
      decisionDoc.h2("Related Decisions");
      decisionDoc.bulletList(
          list -> {
            decision
                .getLinks()
                .forEach(
                    link -> {
                      String linkText = link.getDescription() + " (ID: " + link.getId() + ")";
                      list.item(linkText);
                    });
          });
    }

    String pageTitle = "ADR " + decision.getId() + " - " + decision.getTitle();
    confluenceClient.createOrUpdatePage(
        pageTitle, convertDocumentToJson(decisionDoc), parentPageId);
    logger.info("Created/updated ADR page: {}", pageTitle);
  }

  private boolean isAsciiDocFormat(String formatName) {
    return FORMAT_ASCIIDOC.equalsIgnoreCase(formatName)
        || FORMAT_ASCIIDOC_LOWER.equalsIgnoreCase(formatName);
  }

  private boolean isMarkdownFormat(String formatName) {
    return FORMAT_MARKDOWN.equalsIgnoreCase(formatName)
        || FORMAT_MARKDOWN_SHORT.equalsIgnoreCase(formatName);
  }

  private String getWorkspaceId(Workspace workspace) {
    long id = workspace.getId();
    return id > 0 ? String.valueOf(id) : "unknown";
  }

  private String convertDocumentToJson(Document document) throws Exception {
    return objectMapper.writeValueAsString(document);
  }

  private Document combineDocuments(Document base, Document addition) {
    try {
      String baseJson = convertDocumentToJson(base);
      String addJson = convertDocumentToJson(addition);

      ObjectMapper mapper = new ObjectMapper();
      com.fasterxml.jackson.databind.node.ObjectNode baseNode =
          (com.fasterxml.jackson.databind.node.ObjectNode) mapper.readTree(baseJson);
      com.fasterxml.jackson.databind.node.ObjectNode addNode =
          (com.fasterxml.jackson.databind.node.ObjectNode) mapper.readTree(addJson);

      com.fasterxml.jackson.databind.node.ArrayNode baseContent =
          (com.fasterxml.jackson.databind.node.ArrayNode) baseNode.get("content");
      com.fasterxml.jackson.databind.JsonNode addContentNode = addNode.get("content");
      if (addContentNode != null && addContentNode.isArray()) {
        for (com.fasterxml.jackson.databind.JsonNode child : addContentNode) {
          baseContent.add(child);
        }
      }

      return mapper.treeToValue(baseNode, Document.class);
    } catch (Exception e) {
      logger.warn("Failed to merge ADF documents, keeping base content only", e);
      return base;
    }
  }
}
