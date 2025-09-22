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
        description = "Confluence space key",
        required = true
    )
    String confluenceSpaceKey;

    @CommandLine.Option(
        names = {"-p", "--page-title"}, 
        description = "Target page title (cleans this page and all its subpages)",
        required = true
    )
    String pageTitle;

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
            logger.info("Confluence space: {}", confluenceSpaceKey);
            logger.info("Target page: {}", pageTitle);

            // Create Confluence configuration
            ConfluenceConfig config = new ConfluenceConfig(confluenceUrl, confluenceUser, confluenceToken, confluenceSpaceKey);

            // Create exporter and clean page tree
            ConfluenceExporter exporter = new ConfluenceExporter(config);
            
            logger.info("Cleaning page tree starting from: {}", pageTitle);
            exporter.cleanPageTree(pageTitle);
            logger.info("Page tree cleaning completed");

            System.out.println("✅ Confluence page tree cleaned successfully!");
            
        } catch (Exception e) {
            logger.error("Clean operation failed: {}", e.getMessage(), e);
            System.err.println("❌ Clean failed: " + e.getMessage());
            System.exit(1);
        }
    }

    private boolean promptForConfirmation() {
        System.out.printf("⚠️  This operation will delete the page '%s' and ALL its subpages in space '%s'.%n", pageTitle, confluenceSpaceKey);
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