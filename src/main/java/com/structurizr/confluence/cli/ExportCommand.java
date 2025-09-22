package com.structurizr.confluence.cli;

import com.structurizr.Workspace;
import com.structurizr.confluence.ConfluenceExporter;
import com.structurizr.confluence.client.ConfluenceConfig;
import com.structurizr.util.WorkspaceUtils;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
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
        description = "Confluence space key (required when using --page-title)",
        required = false
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
        description = "Clean target page tree before export",
        defaultValue = "false"
    )
    boolean cleanSpace;

    @CommandLine.Option(
        names = {"--page-title"}, 
        description = "Target page title for cleaning (overrides branch-based target)",
        required = false
    )
    String cleanPageTitle;

    @CommandLine.Option(
        names = {"--page-id"}, 
        description = "Target page ID for cleaning (overrides branch-based target)",
        required = false
    )
    String cleanPageId;

    @CommandLine.Option(
        names = {"-f", "--force"}, 
        description = "Force operation without confirmation prompt",
        defaultValue = "false"
    )
    boolean force;

    @Override
    public void run() {
        try {
            // Validate parameters
            if (cleanPageTitle != null && cleanPageId != null) {
                System.err.println("❌ Error: Cannot specify both --page-title and --page-id");
                System.exit(1);
                return;
            }
            
            if (cleanPageTitle != null && confluenceSpaceKey == null) {
                System.err.println("❌ Error: --confluence-space is required when using --page-title");
                System.exit(1);
                return;
            }
            
            // Space is still required for normal export operations
            if (confluenceSpaceKey == null && cleanPageId == null) {
                System.err.println("❌ Error: --confluence-space is required (except when using --page-id for cleaning only)");
                System.exit(1);
                return;
            }

            logger.info("Starting Structurizr workspace export to Confluence...");
            logger.info("Workspace file: {}", workspaceFile.getAbsolutePath());
            logger.info("Confluence URL: {}", confluenceUrl);
            if (confluenceSpaceKey != null) {
                logger.info("Confluence space: {}", confluenceSpaceKey);
            }
            logger.info("Branch: {}", branchName);

            // Create Confluence configuration
            ConfluenceConfig config = new ConfluenceConfig(confluenceUrl, confluenceUser, confluenceToken, confluenceSpaceKey);

            // Create exporter
            ConfluenceExporter exporter = new ConfluenceExporter(config);

            // Load workspace from file
            logger.info("Loading workspace from: {}", workspaceFile.getAbsolutePath());
            Workspace workspace = WorkspaceUtils.loadWorkspaceFromJson(workspaceFile);
            if (workspace == null) {
                logger.error("Failed to load workspace from file: {}", workspaceFile);
                System.exit(1);
                return;
            }
            logger.info("Workspace loaded successfully: {}", workspace.getName());

            // Clean target page tree if requested
            if (cleanSpace) {
                String targetPageTitle = cleanPageTitle != null ? cleanPageTitle : branchName;
                String targetPageId = cleanPageId;
                
                // Ask for confirmation unless force flag is used
                if (!force) {
                    if (!promptForCleanConfirmation(targetPageTitle, targetPageId)) {
                        System.out.println("❌ Operation cancelled by user");
                        System.exit(1);
                        return;
                    }
                }
                
                if (targetPageId != null) {
                    logger.info("Cleaning target page tree by ID: {}", targetPageId);
                    exporter.cleanPageTreeById(targetPageId);
                } else {
                    logger.info("Cleaning target page tree: {}", targetPageTitle);
                    exporter.cleanPageTree(targetPageTitle);
                }
                logger.info("Page tree cleaning completed");
            }

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

    private boolean promptForCleanConfirmation(String targetPageTitle, String targetPageId) {
        String target;
        String spaceInfo = "";
        
        if (targetPageId != null) {
            target = "page ID '" + targetPageId + "'";
        } else {
            target = "page '" + targetPageTitle + "'";
            if (confluenceSpaceKey != null) {
                spaceInfo = " in space '" + confluenceSpaceKey + "'";
            }
        }
        
        System.out.printf("⚠️  The --clean option will delete the %s%s and ALL its subpages before export.%n", target, spaceInfo);
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