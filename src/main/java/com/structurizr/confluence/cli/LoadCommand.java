package com.structurizr.confluence.cli;

import com.structurizr.confluence.ConfluenceExporter;
import com.structurizr.confluence.client.ConfluenceConfig;
import com.structurizr.confluence.client.StructurizrConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

/**
 * Load command that exports a workspace from Structurizr on-premise to Confluence.
 * Replicates the functionality of StructurizrOnPremiseExample.
 */
@CommandLine.Command(
    name = "load",
    description = "Load workspace from Structurizr on-premise and export to Confluence"
)
public class LoadCommand implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(LoadCommand.class);

    // Structurizr on-premise options
    @CommandLine.Option(
        names = {"--structurizr-url"}, 
        description = "Structurizr on-premise URL (e.g., https://your-structurizr-instance.com)",
        required = true
    )
    String structurizrUrl;

    @CommandLine.Option(
        names = {"--structurizr-key"}, 
        description = "Structurizr API key",
        required = true
    )
    String structurizrApiKey;

    @CommandLine.Option(
        names = {"--structurizr-secret"}, 
        description = "Structurizr API secret",
        required = true
    )
    String structurizrApiSecret;

    @CommandLine.Option(
        names = {"--workspace-id"}, 
        description = "Structurizr workspace ID",
        required = true
    )
    Long workspaceId;

    // Confluence options
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
        names = {"--clean"}, 
        description = "Clean Confluence space before export",
        defaultValue = "false"
    )
    boolean cleanSpace;

    @Override
    public void run() {
        try {
            logger.info("Starting workspace load from Structurizr on-premise...");
            logger.info("Structurizr URL: {}", structurizrUrl);
            logger.info("Workspace ID: {}", workspaceId);
            logger.info("Confluence URL: {}", confluenceUrl);
            logger.info("Confluence space: {}", confluenceSpaceKey);

            // Configure Structurizr on-premise connection
            StructurizrConfig structurizrConfig = new StructurizrConfig(
                structurizrUrl, 
                structurizrApiKey, 
                structurizrApiSecret, 
                workspaceId
            );

            // Configure Confluence connection
            ConfluenceConfig confluenceConfig = new ConfluenceConfig(
                confluenceUrl, 
                confluenceUser, 
                confluenceToken, 
                confluenceSpaceKey
            );

            // Create exporter that loads from Structurizr on-premise
            ConfluenceExporter exporter = new ConfluenceExporter(confluenceConfig, structurizrConfig);

            // Clean space if requested
            if (cleanSpace) {
                logger.info("Cleaning Confluence space: {}", confluenceSpaceKey);
                exporter.cleanConfluenceSpace();
                logger.info("Space cleaning completed");
            }

            // Export workspace (including documentation and ADRs) to Confluence
            logger.info("Starting export from Structurizr on-premise...");
            exporter.exportFromStructurizr();
            logger.info("Export completed successfully!");

            System.out.println("✅ Workspace exported successfully from Structurizr on-premise to Confluence!");
            
        } catch (Exception e) {
            logger.error("Load and export failed: {}", e.getMessage(), e);
            System.err.println("❌ Load and export failed: " + e.getMessage());
            System.exit(1);
        }
    }
}