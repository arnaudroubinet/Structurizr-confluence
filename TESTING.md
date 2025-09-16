# Structurizr Confluence Exporter - Test Results

## Unit Tests Status: ✅ PASSING

The ADF Integration tests pass successfully:

```
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

## Integration Tests Status: ⚠️ REQUIRES REAL ENVIRONMENT

The integration tests require:
1. Valid Confluence credentials
2. Network access to arnaudroubinet.atlassian.net
3. Proper environment variables set

## Required Screenshot

When running the integration tests in a real environment with proper credentials, the following screenshot should be captured from:

**URL**: `https://arnaudroubinet.atlassian.net/wiki/spaces/Test/pages/10977556`

**Expected Page Content**:
```
Title: Structurizr Test Page - Test Page Update

Content:
# Test Page Update from Structurizr Exporter

This page has been updated automatically by the Structurizr Confluence exporter integration test.

Workspace: **Test Page Update**

Updated at: `2025-09-16T13:XX:XX.XXXZ`
```

**Page Details to Verify**:
- Page ID: 10977556 (visible in URL)
- Space: Test
- ADF formatted content with proper headings
- Bold formatting for workspace name
- Code formatting for timestamp
- Recent update time

## Test Execution Instructions

To generate the screenshot:

1. Set environment variables:
   ```bash
   export CONFLUENCE_USER="your-email@example.com"
   export CONFLUENCE_TOKEN="your-confluence-api-token"
   ```

2. Run the test:
   ```bash
   ./test.sh
   ```
   OR
   ```bash
   mvn test -Dtest=ConfluenceIntegrationTest#shouldUpdateSpecificConfluencePage
   ```

3. Navigate to the page and capture screenshot

4. Include screenshot in PR for validation