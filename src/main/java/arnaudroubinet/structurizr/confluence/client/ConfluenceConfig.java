package arnaudroubinet.structurizr.confluence.client;

/**
 * Configuration for connecting to Confluence Cloud.
 *
 * @param baseUrl Confluence base URL
 * @param username Username for authentication
 * @param apiToken API token for authentication
 * @param spaceKey Confluence space key
 */
public record ConfluenceConfig(String baseUrl, String username, String apiToken, String spaceKey) {

  public String getBaseUrl() {
    return baseUrl;
  }

  public String getUsername() {
    return username;
  }

  public String getApiToken() {
    return apiToken;
  }

  public String getSpaceKey() {
    return spaceKey;
  }
}
