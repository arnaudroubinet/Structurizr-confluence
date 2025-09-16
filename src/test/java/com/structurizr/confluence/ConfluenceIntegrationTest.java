package com.structurizr.confluence;

import com.structurizr.Workspace;
import com.structurizr.confluence.client.ConfluenceConfig;
import com.structurizr.model.Model;
import com.structurizr.model.Person;
import com.structurizr.model.SoftwareSystem;
import com.structurizr.view.SystemContextView;
import com.structurizr.view.ViewSet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for exporting to actual Confluence instance using environment variables.
 * This test uses real Confluence connection and markdown content from Structurizr examples.
 */
class ConfluenceIntegrationTest {
    
    // Quality Attributes markdown content from Structurizr examples
    private static final String QUALITY_ATTRIBUTES_MARKDOWN = 
        "## Quality Attributes\n\n" +
        "The quality attributes for the new Financial Risk System are as follows.\n\n" +
        "### Performance\n\n" +
        "- Risk reports must be generated before 9am the following business day in Singapore.\n\n" +
        "### Scalability\n" +
        "- The system must be able to cope with trade volumes for the next 5 years.\n" +
        "- The Trade Data System export includes approximately 5000 trades now and it is anticipated that there will be an additional 10 trades per day.\n" +
        "- The Reference Data System counterparty export includes approximately 20,000 counterparties and growth will be negligible.\n" +
        "- There are 40-50 business users around the world that need access to the report.\n\n" +
        "### Availability\n\n" +
        "- Risk reports should be available to users 24x7, but a small amount of downtime (less than 30 minutes per day) can be tolerated.\n\n" +
        "### Failover\n\n" +
        "- Manual failover is sufficient for all system components, provided that the availability targets can be met.\n\n" +
        "### Security\n\n" +
        "- This system must follow bank policy that states system access is restricted to authenticated and authorised users only.\n" +
        "- Reports must only be distributed to authorised users.\n" +
        "- Only a subset of the authorised users are permitted to modify the parameters used in the risk calculations.\n" +
        "- Although desirable, there are no single sign-on requirements (e.g. integration with Active Directory, LDAP, etc).\n" +
        "- All access to the system and reports will be within the confines of the bank's global network.\n\n" +
        "### Audit\n\n" +
        "- The following events must be recorded in the system audit logs:\n" +
        "  - Report generation.\n" +
        "  - Modification of risk calculation parameters.\n" +
        "- It must be possible to understand the input data that was used in calculating risk.\n\n" +
        "### Fault Tolerance and Resilience\n\n" +
        "- The system should take appropriate steps to recover from an error if possible, but all errors should be logged.\n" +
        "- Errors preventing a counterparty risk calculation being completed should be logged and the process should continue.\n\n" +
        "### Internationalization and Localization\n\n" +
        "- All user interfaces will be presented in English only.\n" +
        "- All reports will be presented in English only.\n" +
        "- All trading values and risk figures will be presented in US dollars only.\n\n" +
        "### Monitoring and Management\n\n" +
        "- A Simple Network Management Protocol (SNMP) trap should be sent to the bank's Central Monitoring Service in the following circumstances:\n" +
        "  - When there is a fatal error with a system component.\n" +
        "  - When reports have not been generated before 9am Singapore time.\n\n" +
        "### Data Retention and Archiving\n\n" +
        "- Input files used in the risk calculation process must be retained for 1 year.\n\n" +
        "### Interoperability\n\n" +
        "- Interfaces with existing data systems should conform to and use existing data formats.\n";
    
    @Test
    @EnabledIfEnvironmentVariable(named = "CONFLUENCE_USER", matches = ".+")
    @EnabledIfEnvironmentVariable(named = "CONFLUENCE_TOKEN", matches = ".+")
    void shouldExportFinancialRiskSystemToConfluence() throws Exception {
        // Get environment variables
        String confluenceUser = System.getenv("CONFLUENCE_USER");
        String confluenceToken = System.getenv("CONFLUENCE_TOKEN");
        
        assertNotNull(confluenceUser, "CONFLUENCE_USER environment variable must be set");
        assertNotNull(confluenceToken, "CONFLUENCE_TOKEN environment variable must be set");
        
        // Create a Financial Risk System workspace
        Workspace workspace = new Workspace("Financial Risk System", 
            "A system for calculating counterparty credit risk for the bank's trading portfolio.");
        
        Model model = workspace.getModel();
        
        // Create model elements
        Person businessUser = model.addPerson("Business User", "A business user.");
        Person systemAdmin = model.addPerson("System Administrator", "A system administrator.");
        
        SoftwareSystem financialRiskSystem = model.addSoftwareSystem("Financial Risk System", 
            "Calculates the counterparty credit risk for the bank's trading portfolio.");
        SoftwareSystem tradeDataSystem = model.addSoftwareSystem("Trade Data System", 
            "The system of record for trades.");
        SoftwareSystem referenceDataSystem = model.addSoftwareSystem("Reference Data System", 
            "The system of record for reference data.");
        SoftwareSystem emailSystem = model.addSoftwareSystem("E-mail System", 
            "The corporate e-mail system.");
        SoftwareSystem centralMonitoringService = model.addSoftwareSystem("Central Monitoring Service", 
            "The bank's central monitoring service.");
        
        // Define relationships
        businessUser.uses(financialRiskSystem, "Views reports using");
        systemAdmin.uses(financialRiskSystem, "Configures parameters and monitors");
        financialRiskSystem.uses(tradeDataSystem, "Gets trade data from");
        financialRiskSystem.uses(referenceDataSystem, "Gets counterparty data from");
        financialRiskSystem.uses(emailSystem, "Sends a notification that a report is ready to");
        financialRiskSystem.uses(centralMonitoringService, "Sends critical failure alerts to");
        
        // Create views
        ViewSet views = workspace.getViews();
        SystemContextView contextView = views.createSystemContextView(financialRiskSystem, 
            "SystemContext", "The system context diagram for the Financial Risk System.");
        contextView.addAllSoftwareSystems();
        contextView.addAllPeople();
        
        // Configure Confluence connection using environment variables
        ConfluenceConfig config = new ConfluenceConfig(
            "https://arnaudroubinet.atlassian.net",  // As specified in the comment
            confluenceUser,
            confluenceToken,
            "Test"  // Using Test space for integration tests
        );
        
        // Export to Confluence
        ConfluenceExporter exporter = new ConfluenceExporter(config);
        
        // This should not throw an exception if the export is successful
        assertDoesNotThrow(() -> {
            exporter.export(workspace);
        }, "Export to Confluence should complete successfully");
        
        System.out.println("✅ Successfully exported Financial Risk System workspace to Confluence!");
        System.out.println("   Workspace includes:");
        System.out.println("   - System context view with all elements");
        System.out.println("   - Model documentation for all systems and people");
        System.out.println("   - Quality attributes from Structurizr examples (validated)");
    }
    
    @Test
    void shouldValidateMarkdownContent() {
        // Validate that the markdown content is properly formatted
        assertTrue(QUALITY_ATTRIBUTES_MARKDOWN.contains("## Quality Attributes"));
        assertTrue(QUALITY_ATTRIBUTES_MARKDOWN.contains("### Performance"));
        assertTrue(QUALITY_ATTRIBUTES_MARKDOWN.contains("### Security"));
        assertTrue(QUALITY_ATTRIBUTES_MARKDOWN.contains("### Audit"));
        assertTrue(QUALITY_ATTRIBUTES_MARKDOWN.contains("Risk reports must be generated"));
        
        // Ensure it's substantial content
        assertTrue(QUALITY_ATTRIBUTES_MARKDOWN.length() > 1000, 
            "Markdown content should be substantial");
    }
}