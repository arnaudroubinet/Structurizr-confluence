package com.structurizr.confluence.cli;

import com.structurizr.confluence.ConfluenceExporter;
import com.structurizr.confluence.client.ConfluenceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Clean command that removes pages from a Confluence page tree.
 */
@CommandLine.Command(
    name = "clean",
    description = "Clean pages from a Confluence page and its subpages"
)
public class CleanCommand implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(CleanCommand.class);

    @CommandLine.Option(
        names = {"-u", "--confluence-url"}, 
        description = "Confluence base URL (e.g., https://yourcompany.atlassian.net)",
        required = true
    )
    String confluenceUrl;

    @CommandLine.Option(
        names = {"-e", "--confluence-user"}, 
        description = "Confluence user email",
        required = true
    )
    String confluenceUser;

    @CommandLine.Option(
        names = {"-t", "--confluence-token"}, 
        description = "Confluence API token",
        required = true
    )
    String confluenceToken;

    @CommandLine.Option(
        names = {"-s", "--confluence-space"}, 
        description = "Confluence space key (required when using --page-title)",
        required = false
    )
    String confluenceSpaceKey;

    @CommandLine.Option(
        names = {"-p", "--page-title"}, 
        description = "Target page title (cleans this page and all its subpages)",
        required = false
    )
    String pageTitle;

    @CommandLine.Option(
        names = {"--page-id"}, 
        description = "Target page ID (cleans this page and all its subpages)",
        required = false
    )
    String pageId;

    @CommandLine.Option(
        names = {"-f", "--force"}, 
        description = "Force deletion without confirmation prompt",
        defaultValue = "false"
    )
    boolean force;

    @CommandLine.Option(
        names = {"--confirm"}, 
        description = "Confirm deletion (deprecated, use -f instead)",
        defaultValue = "false"
    )
    boolean confirmDeletion;

    @Override
    public void run() {
        try {
            // Validate parameters
            if (pageTitle == null && pageId == null) {
                System.err.println("❌ Error: Either --page-title or --page-id must be specified");
                System.exit(1);
                return;
            }
            
            if (pageTitle != null && pageId != null) {
                System.err.println("❌ Error: Cannot specify both --page-title and --page-id");
                System.exit(1);
                return;
            }
            
            if (pageTitle != null && confluenceSpaceKey == null) {
                System.err.println("❌ Error: --confluence-space is required when using --page-title");
                System.exit(1);
                return;
            }

            // Check if confirmation is needed
            if (!force && !confirmDeletion) {
                if (!promptForConfirmation()) {
                    System.out.println("❌ Operation cancelled by user");
                    System.exit(1);
                    return;
                }
            }

            logger.info("Starting Confluence page tree cleanup...");
            logger.info("Confluence URL: {}", confluenceUrl);
            if (confluenceSpaceKey != null) {
                logger.info("Confluence space: {}", confluenceSpaceKey);
            }
            if (pageTitle != null) {
                logger.info("Target page title: {}", pageTitle);
            } else {
                logger.info("Target page ID: {}", pageId);
            }

            // Create Confluence configuration - space can be null for page ID operations
            ConfluenceConfig config = new ConfluenceConfig(confluenceUrl, confluenceUser, confluenceToken, confluenceSpaceKey);

            // Create exporter and clean page tree
            ConfluenceExporter exporter = new ConfluenceExporter(config);
            
            if (pageTitle != null) {
                logger.info("Cleaning page tree starting from title: {}", pageTitle);
                exporter.cleanPageTree(pageTitle);
            } else {
                logger.info("Cleaning page tree starting from ID: {}", pageId);
                exporter.cleanPageTreeById(pageId);
            }
            logger.info("Page tree cleaning completed");

            System.out.println("✅ Confluence page tree cleaned successfully!");
            
        } catch (Exception e) {
            logger.error("Clean operation failed: {}", e.getMessage(), e);
            System.err.println("❌ Clean failed: " + e.getMessage());
            System.exit(1);
        }
    }

    private boolean promptForConfirmation() {
        String target = pageTitle != null ? "page '" + pageTitle + "'" : "page ID '" + pageId + "'";
        String spaceInfo = confluenceSpaceKey != null ? " in space '" + confluenceSpaceKey + "'" : "";
        
        System.out.printf("⚠️  This operation will delete the %s%s and ALL its subpages.%n", target, spaceInfo);
        System.out.print("Are you sure you want to continue? (yes/no): ");
        
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
            String response = reader.readLine();
            return "yes".equalsIgnoreCase(response) || "y".equalsIgnoreCase(response);
        } catch (IOException e) {
            logger.error("Failed to read user input: {}", e.getMessage());
            return false;
        }
    }
}