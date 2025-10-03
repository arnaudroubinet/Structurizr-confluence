package arnaudroubinet.structurizr.confluence.client;

import jakarta.annotation.Priority;
import jakarta.inject.Singleton;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Singleton
@Priority(Priorities.AUTHENTICATION)
public class AuthHeadersFilter implements ClientRequestFilter {

  @ConfigProperty(name = "confluence.username")
  String username;

  @ConfigProperty(name = "confluence.token")
  String token;

  public AuthHeadersFilter() {}

  public AuthHeadersFilter(String username, String token) {
    this.username = username;
    this.token = token;
  }

  @Override
  public void filter(ClientRequestContext requestContext) throws IOException {
    String basic = username + ":" + token;
    String header =
        "Basic " + Base64.getEncoder().encodeToString(basic.getBytes(StandardCharsets.UTF_8));
    requestContext.getHeaders().putSingle("Authorization", header);
    // Confluence recommends this header for multipart
    requestContext.getHeaders().putSingle("X-Atlassian-Token", "nocheck");
  }
}
