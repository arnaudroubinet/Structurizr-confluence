# Structurizr Authentication Troubleshooting

This document provides guidance for troubleshooting authentication issues when connecting to Structurizr on-premise instances.

## Common Authentication Error

When the Structurizr Confluence Exporter attempts to load a workspace from an on-premise Structurizr instance and encounters authentication issues, you may see an error like:

```
JsonParseException: Unexpected character ('<' (code 60)): expected a valid value (JSON String, Number, Array, Object or token 'null', 'true' or 'false')
```

Or with enhanced error handling (v1.0.0+):

```
Failed to load workspace from Structurizr instance. The server returned an HTML response instead of JSON, 
which typically indicates an authentication problem.
```

## What This Means

This error occurs when:

1. The application makes an API request to `/workspace/{id}`
2. The Structurizr server responds with HTTP 303 (redirect) to `/signin`
3. The application follows the redirect and receives an HTML login page
4. The JSON parser tries to parse the HTML and fails

**Root Cause**: The Structurizr on-premise instance is protecting the workspace API with web-based authentication, and the API key authentication is not sufficient or not properly configured.

## Understanding Structurizr Authentication

Structurizr on-premise instances can be configured with different authentication methods:

### API Key Authentication (for API access)
- Used by the Workspace API for programmatic access
- Configured with `STRUCTURIZR_API_KEY` and `STRUCTURIZR_API_SECRET`
- Sends `X-Authorization` header with HMAC-signed requests

### Web Authentication (for user access)
- Used by the web interface for user login
- Can be configured with File, LDAP, or SAML authentication
- Requires username and password through a web form

## Common Causes and Solutions

### 1. Incorrect API Key or Secret

**Symptom**: Server redirects to login page immediately

**Solution**: Verify your API credentials:

```bash
# Check your environment variables
echo $STRUCTURIZR_API_KEY
echo $STRUCTURIZR_API_SECRET

# Or check your command-line arguments
--structurizr-api-key=<your-key>
--structurizr-api-secret=<your-secret>
```

To obtain valid API credentials:
1. Log in to your Structurizr on-premise web interface
2. Navigate to your workspace settings
3. Find the API key and secret under "API Settings"
4. Copy these values exactly (they are case-sensitive)

### 2. Workspace Does Not Have API Access Enabled

**Symptom**: Server redirects to login even with valid credentials

**Solution**: Check workspace API settings:

1. Log in to Structurizr web interface
2. Open your workspace
3. Go to workspace settings
4. Verify that "API Access" is enabled
5. If disabled, enable it and note the API key/secret

### 3. On-Premise Instance Requires Additional Authentication

**Symptom**: API key is valid but server still redirects

**Solution**: Some on-premise installations are configured to require both API key authentication AND web-based user authentication. In this case:

1. **Contact your Structurizr administrator** to verify the authentication configuration
2. Ask if API-only access is supported
3. Request that API access be enabled for your workspace

The administrator may need to adjust the Structurizr configuration:

```properties
# In structurizr.properties
structurizr.authentication=<method>  # file, ldap, or saml
structurizr.feature.workspace.api=true  # Ensure API access is enabled
```

### 4. Network or Proxy Issues

**Symptom**: Intermittent authentication failures

**Solution**: 

1. Verify network connectivity to the Structurizr instance
2. Check if a proxy is interfering with API requests
3. If using HTTPS with self-signed certificates, use the `--disable-ssl-verification` flag (development only)

```bash
export DISABLE_SSL_VERIFICATION=true
# Or use command-line flag
--disable-ssl-verification
```

See [SSL_BYPASS.md](SSL_BYPASS.md) for more details on SSL certificate handling.

## Debugging with Debug Mode

Enable debug mode to see detailed HTTP request/response logging:

```bash
# Via environment variable
export DEBUG_MODE=true

# Or via command-line flag
--debug
```

With debug mode enabled, you'll see:
- HTTP request headers (including X-Authorization)
- HTTP response codes (e.g., 303 redirect)
- Response body content
- Detailed error messages

See [HTTP_LOGGING.md](HTTP_LOGGING.md) for more details on debug logging.

## Example Debug Output

When authentication fails, debug mode will show:

```
[StructurizrWorkspaceLoader] Loading workspace 2 from Structurizr instance
[headers] http-outgoing-0 >> GET /workspace/2 HTTP/1.1
[headers] http-outgoing-0 >> X-Authorization: [key]:[hash]
[headers] http-outgoing-0 << HTTP/1.1 303
[headers] http-outgoing-0 << Location: https://structurizr.example.com/signin
```

This clearly shows:
1. The API request is made
2. The server responds with 303 (redirect)
3. The location header points to the sign-in page

## Verification Steps

To verify your Structurizr API access is working:

1. **Test API Key Format**:
   ```bash
   # API key should be a long alphanumeric string
   # API secret should be a long alphanumeric string
   # Both are case-sensitive
   ```

2. **Test API Endpoint Directly** (using curl):
   ```bash
   # Calculate the X-Authorization header (complex, see Structurizr docs)
   # Or use the official Structurizr CLI to test access:
   curl -H "X-Authorization: [key]:[hash]" \
        -H "Nonce: [timestamp]" \
        https://your-structurizr-instance/api/workspace/[id]
   ```

3. **Contact Your Administrator**:
   - Verify the workspace exists and you have access
   - Confirm API access is enabled for your workspace
   - Check if there are any additional authentication requirements

## Related Documentation

- [HTTP_LOGGING.md](HTTP_LOGGING.md) - Debug logging for HTTP requests
- [SSL_BYPASS.md](SSL_BYPASS.md) - SSL certificate handling
- [Structurizr On-Premise Documentation](https://docs.structurizr.com/onpremises) - Official Structurizr documentation
- [Structurizr Workspace API](https://docs.structurizr.com/onpremises/workspace-api) - API authentication details

## Support

If you continue to experience authentication issues after following this guide:

1. Enable debug mode and capture the full log output
2. Verify with your Structurizr administrator that API access is properly configured
3. Check the Structurizr server logs for authentication errors
4. Open an issue on GitHub with:
   - Structurizr version
   - Authentication method used (file/LDAP/SAML)
   - Debug log output (with sensitive data redacted)
   - Error messages received
