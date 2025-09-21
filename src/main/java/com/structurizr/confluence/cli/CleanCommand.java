package com.structurizr.confluence.cli;

import com.structurizr.confluence.ConfluenceExporter;
import com.structurizr.confluence.client.ConfluenceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

/**
 * Clean command that removes all pages from a Confluence space.
 */
@CommandLine.Command(
    name = "clean",
    description = "Clean all pages from a Confluence space"
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
        names = {"--confirm"}, 
        description = "Confirm deletion of all pages in the space",
        defaultValue = "false"
    )
    boolean confirmDeletion;

    @Override
    public void run() {
        try {
            if (!confirmDeletion) {
                System.err.println("❌ This operation will delete ALL pages in the Confluence space: " + confluenceSpaceKey);
                System.err.println("❌ Use --confirm flag to proceed with deletion");
                System.exit(1);
                return;
            }

            logger.info("Starting Confluence space cleanup...");
            logger.info("Confluence URL: {}", confluenceUrl);
            logger.info("Confluence space: {}", confluenceSpaceKey);

            // Create Confluence configuration
            ConfluenceConfig config = new ConfluenceConfig(confluenceUrl, confluenceUser, confluenceToken, confluenceSpaceKey);

            // Create exporter and clean space
            ConfluenceExporter exporter = new ConfluenceExporter(config);
            
            logger.info("Cleaning Confluence space: {}", confluenceSpaceKey);
            exporter.cleanConfluenceSpace();
            logger.info("Space cleaning completed");

            System.out.println("✅ Confluence space cleaned successfully!");
            
        } catch (Exception e) {
            logger.error("Clean operation failed: {}", e.getMessage(), e);
            System.err.println("❌ Clean failed: " + e.getMessage());
            System.exit(1);
        }
    }
}