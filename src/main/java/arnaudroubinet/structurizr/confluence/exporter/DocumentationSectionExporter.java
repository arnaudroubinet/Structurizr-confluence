package arnaudroubinet.structurizr.confluence.exporter;

import arnaudroubinet.structurizr.confluence.client.ConfluenceClient;
import arnaudroubinet.structurizr.confluence.processor.AsciiDocConverter;
import arnaudroubinet.structurizr.confluence.processor.HtmlToAdfConverter;
import arnaudroubinet.structurizr.confluence.processor.ImageUploadManager;
import arnaudroubinet.structurizr.confluence.processor.MarkdownConverter;
import com.structurizr.Workspace;
import com.structurizr.documentation.Section;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles export of workspace documentation sections to Confluence. Separates documentation section
 * export logic from main export orchestration.
 */
public class DocumentationSectionExporter {

  private static final Logger logger = LoggerFactory.getLogger(DocumentationSectionExporter.class);

  private static final String FORMAT_ASCIIDOC = "AsciiDoc";
  private static final String FORMAT_ASCIIDOC_LOWER = "asciidoc";
  private static final String FORMAT_MARKDOWN = "Markdown";
  private static final String FORMAT_MARKDOWN_SHORT = "md";

  private final ConfluenceClient confluenceClient;
  private final HtmlToAdfConverter htmlToAdfConverter;
  private final AsciiDocConverter asciiDocConverter;
  private final MarkdownConverter markdownConverter;

  public DocumentationSectionExporter(
      ConfluenceClient confluenceClient,
      HtmlToAdfConverter htmlToAdfConverter,
      AsciiDocConverter asciiDocConverter,
      MarkdownConverter markdownConverter) {
    this.confluenceClient = confluenceClient;
    this.htmlToAdfConverter = htmlToAdfConverter;
    this.asciiDocConverter = asciiDocConverter;
    this.markdownConverter = markdownConverter;
  }

  /**
   * Exports all documentation sections from a workspace to Confluence.
   *
   * @param workspace the workspace containing documentation sections
   * @param parentPageId the parent page ID where sections will be created
   * @param branchName the branch name for context
   * @throws Exception if export fails
   */
  public void exportWorkspaceDocumentationSections(
      Workspace workspace, String parentPageId, String branchName) throws Exception {
    if (workspace.getDocumentation() == null
        || workspace.getDocumentation().getSections().isEmpty()) {
      logger.info("No documentation sections found in workspace");
      return;
    }

    logger.info(
        "Export des sections de documentation du workspace '{}', {} section(s)",
        workspace.getName(),
        workspace.getDocumentation().getSections().size());

    // Export each section as Confluence page
    for (Section section : workspace.getDocumentation().getSections()) {
      exportSection(section, parentPageId, workspace, branchName);
    }
  }

  /**
   * Exports a single documentation section to Confluence.
   *
   * @param section the section to export
   * @param parentPageId the parent page ID
   * @param workspace the workspace for context
   * @param branchName the branch name
   * @throws Exception if export fails
   */
  private void exportSection(
      Section section, String parentPageId, Workspace workspace, String branchName)
      throws Exception {
    String filenameFallback = section.getFilename();
    String content = section.getContent();

    String htmlContent;
    String formatName = section.getFormat() != null ? section.getFormat().name() : "";

    if (isAsciiDocFormat(formatName)) {
      logger.debug("Converting AsciiDoc content for section (filename: {})", filenameFallback);
      String workspaceId = getWorkspaceId(workspace);
      htmlContent =
          asciiDocConverter.convertToHtml(content, filenameFallback, workspaceId, branchName);
    } else if (isMarkdownFormat(formatName)) {
      logger.debug(
          "Markdown content detected for section (filename: {}): converting to HTML for title extraction",
          filenameFallback);
      htmlContent = markdownConverter.toHtml(content);
    } else {
      logger.debug(
          "Treating content as HTML for section (filename: {}), format: {}",
          filenameFallback,
          formatName);
      htmlContent = content;
    }

    // Extract title from HTML content (first H1) if available
    String extractedTitle = htmlToAdfConverter.extractPageTitleOnly(htmlContent);
    String actualTitle =
        (extractedTitle != null && !extractedTitle.trim().isEmpty())
            ? extractedTitle
            : filenameFallback;

    // Setup image upload manager for this page
    ImageUploadManager imageUploadManager = new ImageUploadManager(confluenceClient);
    htmlToAdfConverter.setImageUploadManager(imageUploadManager);

    // Create page first to get the page ID for image uploads
    String pageTitle = actualTitle;
    String pageId =
        confluenceClient.createOrUpdatePage(
            pageTitle, "{\"version\":1,\"type\":\"doc\",\"content\":[]}", parentPageId);

    // Set page context for image uploads
    htmlToAdfConverter.setCurrentPageId(pageId);

    // Convert HTML to ADF JSON for Confluence with native table support
    String adfJson = htmlToAdfConverter.convertToAdfJson(htmlContent, actualTitle);

    // Update page with actual content
    confluenceClient.updatePageById(pageId, pageTitle, adfJson);
    logger.info(
        "Section exported to page ID: {} avec le titre: '{}'", filenameFallback, pageId, pageTitle);
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
}
