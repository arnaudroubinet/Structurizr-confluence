package arnaudroubinet.structurizr.confluence.client;

/**
 * Configuration for connecting to a Structurizr on-premise instance.
 *
 * @param apiUrl Structurizr API URL
 * @param apiKey API key for authentication
 * @param apiSecret API secret for authentication
 * @param workspaceId Workspace ID to load
 * @param debugMode Enable debug mode for detailed logging
 */
public record StructurizrConfig(
    String apiUrl, String apiKey, String apiSecret, long workspaceId, boolean debugMode) {

  public StructurizrConfig(String apiUrl, String apiKey, String apiSecret, long workspaceId) {
    this(apiUrl, apiKey, apiSecret, workspaceId, false);
  }

  public String getApiUrl() {
    return apiUrl;
  }

  public String getApiKey() {
    return apiKey;
  }

  public String getApiSecret() {
    return apiSecret;
  }

  public long getWorkspaceId() {
    return workspaceId;
  }

  public boolean isDebugMode() {
    return debugMode;
  }
}
