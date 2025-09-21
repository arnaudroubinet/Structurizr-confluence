package com.structurizr.confluence.cli;

import com.structurizr.Workspace;
import com.structurizr.confluence.ConfluenceExporter;
import com.structurizr.confluence.client.ConfluenceConfig;
import com.structurizr.util.WorkspaceUtils;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.File;
import java.nio.file.Path;

/**
 * Export command that exports a Structurizr workspace to Confluence.
 * Replicates the functionality of ConfluenceExporterIntegrationTest.
 */
@CommandLine.Command(
    name = "export",
    description = "Export a Structurizr workspace to Confluence Cloud"
)
public class ExportCommand implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(ExportCommand.class);

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
        names = {"-w", "--workspace"}, 
        description = "Path to Structurizr workspace JSON file",
        required = true
    )
    File workspaceFile;

    @CommandLine.Option(
        names = {"-b", "--branch"}, 
        description = "Branch name for versioning",
        defaultValue = "main"
    )
    String branchName;

    @CommandLine.Option(
        names = {"--clean"}, 
        description = "Clean Confluence space before export",
        defaultValue = "false"
    )
    boolean cleanSpace;

    @Override
    public void run() {
        try {
            logger.info("Starting Structurizr workspace export to Confluence...");
            logger.info("Workspace file: {}", workspaceFile.getAbsolutePath());
            logger.info("Confluence URL: {}", confluenceUrl);
            logger.info("Confluence space: {}", confluenceSpaceKey);
            logger.info("Branch: {}", branchName);

            // Create Confluence configuration
            ConfluenceConfig config = new ConfluenceConfig(confluenceUrl, confluenceUser, confluenceToken, confluenceSpaceKey);

            // Create exporter
            ConfluenceExporter exporter = new ConfluenceExporter(config);

            // Clean space if requested
            if (cleanSpace) {
                logger.info("Cleaning Confluence space: {}", confluenceSpaceKey);
                exporter.cleanConfluenceSpace();
                logger.info("Space cleaning completed");
            }

            // Load workspace from file
            logger.info("Loading workspace from: {}", workspaceFile.getAbsolutePath());
            Workspace workspace = WorkspaceUtils.loadWorkspaceFromJson(workspaceFile);
            if (workspace == null) {
                logger.error("Failed to load workspace from file: {}", workspaceFile);
                System.exit(1);
                return;
            }
            logger.info("Workspace loaded successfully: {}", workspace.getName());

            // Export workspace
            logger.info("Starting workspace export...");
            exporter.export(workspace, branchName);
            logger.info("Export completed successfully!");

            System.out.println("✅ Workspace exported successfully to Confluence!");
            
        } catch (Exception e) {
            logger.error("Export failed: {}", e.getMessage(), e);
            System.err.println("❌ Export failed: " + e.getMessage());
            System.exit(1);
        }
    }
}