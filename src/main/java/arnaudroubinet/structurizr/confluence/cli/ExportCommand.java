package arnaudroubinet.structurizr.confluence.cli;

import com.structurizr.Workspace;
import arnaudroubinet.structurizr.confluence.ConfluenceExporter;
import arnaudroubinet.structurizr.confluence.client.ConfluenceConfig;
import arnaudroubinet.structurizr.confluence.client.StructurizrConfig;
import arnaudroubinet.structurizr.confluence.util.SslTrustUtils;
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

    // Confluence options
    @CommandLine.Option(
        names = {"-u", "--confluence-url"}, 
        description = "Confluence base URL (default: CONFLUENCE_URL env var)",
        required = false
    )
    String confluenceUrl;

    @CommandLine.Option(
        names = {"-e", "--confluence-user"}, 
        description = "Confluence user email (default: CONFLUENCE_USER env var)",
        required = false
    )
    String confluenceUser;

    @CommandLine.Option(
        names = {"-t", "--confluence-token"}, 
        description = "Confluence API token (default: CONFLUENCE_TOKEN env var)",
        required = false
    )
    String confluenceToken;

    @CommandLine.Option(
        names = {"-s", "--confluence-space"}, 
        description = "Confluence space key (default: CONFLUENCE_SPACE_KEY env var, required when using --page-title)",
        required = false
    )
    String confluenceSpaceKey;

    // Workspace source options (mutually exclusive)
    @CommandLine.Option(
        names = {"-w", "--workspace-file"}, 
        description = "Path to Structurizr workspace JSON file (takes priority over Structurizr options)",
        required = false
    )
    File workspaceFile;

    // Structurizr on-premise options
    @CommandLine.Option(
        names = {"--structurizr-url"}, 
        description = "Structurizr on-premise URL (default: STRUCTURIZR_URL env var)",
        required = false
    )
    String structurizrUrl;

    @CommandLine.Option(
        names = {"--structurizr-key"}, 
        description = "Structurizr API key (default: STRUCTURIZR_API_KEY env var)",
        required = false
    )
    String structurizrApiKey;

    @CommandLine.Option(
        names = {"--structurizr-secret"}, 
        description = "Structurizr API secret (default: STRUCTURIZR_API_SECRET env var)",
        required = false
    )
    String structurizrApiSecret;

    @CommandLine.Option(
        names = {"--structurizr-workspace-id"}, 
        description = "Structurizr workspace ID (default: STRUCTURIZR_WORKSPACE_ID env var)",
        required = false
    )
    Long structurizrWorkspaceId;

    @CommandLine.Option(
        names = {"-b", "--branch"}, 
        description = "Branch name for versioning (required)",
        required = true
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
        description = "Parent page ID where branch subpage will be created (required)",
        required = true
    )
    String pageId;

    @CommandLine.Option(
        names = {"-f", "--force"}, 
        description = "Force operation without confirmation prompt",
        defaultValue = "false"
    )
    boolean force;

    @CommandLine.Option(
        names = {"--disable-ssl-verification"}, 
        description = "Disable SSL certificate verification (useful for self-signed certificates)",
        defaultValue = "false"
    )
    boolean disableSslVerification;

    @CommandLine.Option(
        names = {"--debug"}, 
        description = "Enable debug mode for HTTP request/response logging",
        defaultValue = "false"
    )
    boolean debugMode;

    @Override
    public void run() {
        try {
            // Load configuration from environment variables if not provided
            loadConfigurationFromEnvironment();
            
            // Configure debug mode if enabled
            if (debugMode) {
                logger.info("Debug mode enabled - detailed HTTP logging will be shown");
            }
            
            // Configure SSL trust settings if needed
            if (disableSslVerification) {
                System.setProperty("disable.ssl.verification", "true");
                logger.warn("SSL certificate verification disabled via command line option");
            }
            
            // Validate configuration
            if (!validateConfiguration()) {
                System.exit(1);
                return;
            }

            // Validate page targeting parameters
            if (cleanPageTitle != null && pageId != null) {
                System.err.println("❌ Error: Cannot specify both --page-title and --page-id for cleaning");
                System.exit(1);
                return;
            }
            
            if (cleanPageTitle != null && confluenceSpaceKey == null) {
                System.err.println("❌ Error: --confluence-space is required when using --page-title");
                System.exit(1);
                return;
            }

            logger.info("Starting Structurizr workspace export to Confluence...");
            logConfiguration();

            // Create Confluence configuration
            ConfluenceConfig confluenceConfig = new ConfluenceConfig(confluenceUrl, confluenceUser, confluenceToken, confluenceSpaceKey);

            // Create exporter based on workspace source
            ConfluenceExporter exporter;
            Workspace workspace = null;

            if (workspaceFile != null) {
                // Load workspace from file
                logger.info("Loading workspace from file: {}", workspaceFile.getAbsolutePath());
                workspace = WorkspaceUtils.loadWorkspaceFromJson(workspaceFile);
                if (workspace == null) {
                    logger.error("Failed to load workspace from file: {}", workspaceFile);
                    System.exit(1);
                    return;
                }
                logger.info("Workspace loaded successfully: {}", workspace.getName());
                exporter = new ConfluenceExporter(confluenceConfig);
            } else {
                // Load workspace from Structurizr on-premise
                logger.info("Loading workspace from Structurizr on-premise: {}", structurizrUrl);
                logger.info("Workspace ID: {}", structurizrWorkspaceId);
                
                StructurizrConfig structurizrConfig = new StructurizrConfig(
                    structurizrUrl, 
                    structurizrApiKey, 
                    structurizrApiSecret, 
                    structurizrWorkspaceId,
                    debugMode
                );
                exporter = new ConfluenceExporter(confluenceConfig, structurizrConfig);
            }

            // Clean target page tree if requested
            if (cleanSpace) {
                String targetPageTitle = cleanPageTitle;
                String targetPageId = cleanPageTitle != null ? null : pageId;
                
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
            logger.info("Starting workspace export with parent page ID: {} and branch: {}", pageId, branchName);
            if (workspace != null) {
                exporter.export(workspace, pageId, branchName);
            } else {
                exporter.exportFromStructurizr(pageId, branchName);
            }
            logger.info("Export completed successfully!");

            System.out.println("✅ Workspace exported successfully to Confluence!");
            
        } catch (Exception e) {
            logger.error("Export failed: {}", e.getMessage(), e);
            System.err.println("❌ Export failed: " + e.getMessage());
            System.exit(1);
        }
    }

    private void loadConfigurationFromEnvironment() {
        // Load Confluence configuration from environment variables
        if (confluenceUrl == null) {
            confluenceUrl = System.getenv("CONFLUENCE_URL");
            if (confluenceUrl != null) {
                logger.info("Using CONFLUENCE_URL environment variable");
            }
        }
        
        if (confluenceUser == null) {
            confluenceUser = System.getenv("CONFLUENCE_USER");
            if (confluenceUser != null) {
                logger.info("Using CONFLUENCE_USER environment variable");
            }
        }
        
        if (confluenceToken == null) {
            confluenceToken = System.getenv("CONFLUENCE_TOKEN");
            if (confluenceToken != null) {
                logger.info("Using CONFLUENCE_TOKEN environment variable");
            }
        }
        
        if (confluenceSpaceKey == null) {
            confluenceSpaceKey = System.getenv("CONFLUENCE_SPACE_KEY");
            if (confluenceSpaceKey != null) {
                logger.info("Using CONFLUENCE_SPACE_KEY environment variable");
            }
        }

        // Load Structurizr configuration from environment variables (only if no workspace file provided)
        if (workspaceFile == null) {
            if (structurizrUrl == null) {
                structurizrUrl = System.getenv("STRUCTURIZR_URL");
                if (structurizrUrl != null) {
                    logger.info("Using STRUCTURIZR_URL environment variable");
                }
            }
            
            if (structurizrApiKey == null) {
                structurizrApiKey = System.getenv("STRUCTURIZR_API_KEY");
                if (structurizrApiKey != null) {
                    logger.info("Using STRUCTURIZR_API_KEY environment variable");
                }
            }
            
            if (structurizrApiSecret == null) {
                structurizrApiSecret = System.getenv("STRUCTURIZR_API_SECRET");
                if (structurizrApiSecret != null) {
                    logger.info("Using STRUCTURIZR_API_SECRET environment variable");
                }
            }
            
            if (structurizrWorkspaceId == null) {
                String workspaceIdStr = System.getenv("STRUCTURIZR_WORKSPACE_ID");
                if (workspaceIdStr != null) {
                    try {
                        structurizrWorkspaceId = Long.parseLong(workspaceIdStr);
                        logger.info("Using STRUCTURIZR_WORKSPACE_ID environment variable");
                    } catch (NumberFormatException e) {
                        logger.warn("Invalid STRUCTURIZR_WORKSPACE_ID environment variable: {}", workspaceIdStr);
                    }
                }
            }
        }
    }

    private boolean validateConfiguration() {
        // Validate Confluence configuration
        if (confluenceUrl == null || confluenceUrl.trim().isEmpty()) {
            System.err.println("❌ Error: --confluence-url is required (or set CONFLUENCE_URL environment variable)");
            return false;
        }
        
        if (confluenceUser == null || confluenceUser.trim().isEmpty()) {
            System.err.println("❌ Error: --confluence-user is required (or set CONFLUENCE_USER environment variable)");
            return false;
        }
        
        if (confluenceToken == null || confluenceToken.trim().isEmpty()) {
            System.err.println("❌ Error: --confluence-token is required (or set CONFLUENCE_TOKEN environment variable)");
            return false;
        }

        // Validate workspace source
        if (workspaceFile == null) {
            // Using Structurizr on-premise, validate those parameters
            if (structurizrUrl == null || structurizrUrl.trim().isEmpty()) {
                System.err.println("❌ Error: --structurizr-url is required when not using --workspace-file (or set STRUCTURIZR_URL environment variable)");
                return false;
            }
            
            if (structurizrApiKey == null || structurizrApiKey.trim().isEmpty()) {
                System.err.println("❌ Error: --structurizr-key is required when not using --workspace-file (or set STRUCTURIZR_API_KEY environment variable)");
                return false;
            }
            
            if (structurizrApiSecret == null || structurizrApiSecret.trim().isEmpty()) {
                System.err.println("❌ Error: --structurizr-secret is required when not using --workspace-file (or set STRUCTURIZR_API_SECRET environment variable)");
                return false;
            }
            
            if (structurizrWorkspaceId == null) {
                System.err.println("❌ Error: --structurizr-workspace-id is required when not using --workspace-file (or set STRUCTURIZR_WORKSPACE_ID environment variable)");
                return false;
            }
        }

        // Space is required for normal export operations unless using page-id which can work without space
        if (confluenceSpaceKey == null && pageId == null) {
            System.err.println("❌ Error: --confluence-space is required (or set CONFLUENCE_SPACE_KEY environment variable)");
            return false;
        }

        return true;
    }

    private void logConfiguration() {
        if (workspaceFile != null) {
            logger.info("Workspace source: File - {}", workspaceFile.getAbsolutePath());
        } else {
            logger.info("Workspace source: Structurizr on-premise - {}", structurizrUrl);
            logger.info("Structurizr workspace ID: {}", structurizrWorkspaceId);
        }
        
        logger.info("Confluence URL: {}", confluenceUrl);
        if (confluenceSpaceKey != null) {
            logger.info("Confluence space: {}", confluenceSpaceKey);
        }
        logger.info("Parent page ID: {}", pageId);
        logger.info("Branch: {}", branchName);
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