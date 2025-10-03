package arnaudroubinet.structurizr.confluence.cli;

import io.quarkus.picocli.runtime.annotations.TopCommand;
import picocli.CommandLine;

/**
 * Main CLI entry point for Structurizr Confluence Exporter. Provides command-line interface for
 * exporting Structurizr workspaces to Confluence.
 */
@TopCommand
@CommandLine.Command(
    name = "structurizr-confluence",
    description = "Export Structurizr workspaces to Confluence Cloud",
    mixinStandardHelpOptions = true,
    subcommands = {ExportCommand.class})
public class StructurizrConfluenceCommand {
  // Main entry point - delegates to subcommands
}
