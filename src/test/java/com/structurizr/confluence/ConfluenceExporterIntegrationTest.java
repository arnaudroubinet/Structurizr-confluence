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
        Assumptions.assumeTrue(confluenceUser != null && !confluenceUser.isBlank(), "CONFLUENCE_USER non défini: test ignoré");
        Assumptions.assumeTrue(confluenceUrl != null && !confluenceUrl.isBlank(), "CONFLUENCE_URL non défini: test ignoré");
        Assumptions.assumeTrue(confluenceToken != null && !confluenceToken.isBlank(), "CONFLUENCE_TOKEN non défini: test ignoré");
        Assumptions.assumeTrue(confluenceSpaceKey != null && !confluenceSpaceKey.isBlank(), "CONFLUENCE_SPACE_KEY non défini: test ignoré");
        assertNotNull(confluenceSpaceKey, "CONFLUENCE_SPACE_KEY doit être défini");
        
        ConfluenceConfig config = new ConfluenceConfig(confluenceUrl, confluenceUser, confluenceToken, confluenceSpaceKey);

        // Nettoyer l'espace Confluence avant l'export
        ConfluenceExporter exporter = new ConfluenceExporter(config);
        System.out.println("Nettoyage de l'espace Confluence: " + confluenceSpaceKey);
        exporter.cleanConfluenceSpace();
        System.out.println("Nettoyage terminé");

        File file = Path.of("demo/itms-workspace.json").toFile();
        Workspace workspace = WorkspaceUtils.loadWorkspaceFromJson(file);
        assertNotNull(workspace);

        exporter.export(workspace);
    }
}
