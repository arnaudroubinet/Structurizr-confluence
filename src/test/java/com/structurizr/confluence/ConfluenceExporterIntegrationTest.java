package com.structurizr.confluence;

import com.structurizr.Workspace;
import com.structurizr.confluence.client.ConfluenceConfig;
import com.structurizr.util.WorkspaceUtils;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class ConfluenceExporterIntegrationTest {
    @Test
    public void exportWorkspaceToConfluence() throws Exception {
        String confluenceUser = System.getenv("CONFLUENCE_USER");
        String confluenceUrl = System.getenv("CONFLUENCE_URL");
        String confluenceToken = System.getenv("CONFLUENCE_TOKEN");
        String confluenceSpaceKey = System.getenv("CONFLUENCE_SPACE_KEY");
        Assumptions.assumeTrue(confluenceUser != null && !confluenceUser.isBlank(), "CONFLUENCE_USER not defined: test skipped");
        Assumptions.assumeTrue(confluenceUrl != null && !confluenceUrl.isBlank(), "CONFLUENCE_URL not defined: test skipped");
        Assumptions.assumeTrue(confluenceToken != null && !confluenceToken.isBlank(), "CONFLUENCE_TOKEN not defined: test skipped");
        Assumptions.assumeTrue(confluenceSpaceKey != null && !confluenceSpaceKey.isBlank(), "CONFLUENCE_SPACE_KEY not defined: test skipped");
        assertNotNull(confluenceSpaceKey, "CONFLUENCE_SPACE_KEY must be defined");
        
        ConfluenceConfig config = new ConfluenceConfig(confluenceUrl, confluenceUser, confluenceToken, confluenceSpaceKey);

        // Clean the Confluence space before export
        ConfluenceExporter exporter = new ConfluenceExporter(config);
        System.out.println("Cleaning Confluence space: " + confluenceSpaceKey);
        exporter.cleanConfluenceSpace();
        System.out.println("Cleaning completed");

        File file = Path.of("demo/itms-workspace.json").toFile();
        Workspace workspace = WorkspaceUtils.loadWorkspaceFromJson(file);
        assertNotNull(workspace);

        exporter.export(workspace);
    }
}
