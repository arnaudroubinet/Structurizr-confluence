package arnaudroubinet.structurizr.confluence.client;

/** Configuration for connecting to Confluence Cloud. */
public class ConfluenceConfig {

  private final String baseUrl;
  private final String username;
  private final String apiToken;
  private final String spaceKey;

  public ConfluenceConfig(String baseUrl, String username, String apiToken, String spaceKey) {
    this.baseUrl = baseUrl;
    this.username = username;
    this.apiToken = apiToken;
    this.spaceKey = spaceKey;
  }

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
