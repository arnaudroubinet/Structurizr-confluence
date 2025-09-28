# SSL Certificate Verification Bypass

This document explains how to use the SSL certificate verification bypass functionality when working with self-signed certificates.

## Problem

When using self-signed certificates, you may encounter the error:
```
ERR_CERT_AUTHORITY_INVALID when calling structurizer during the schema export
```

## Solution

The application now provides several ways to disable SSL certificate verification:

### 1. Command Line Option
```bash
java -jar structurizr-confluence.jar export --disable-ssl-verification \
  --confluence-url https://your-confluence.example.com \
  --confluence-user your-email@example.com \
  --confluence-token your-api-token \
  --confluence-space YOUR_SPACE \
  --structurizr-url https://your-structurizr.example.com \
  --structurizr-key your-api-key \
  --structurizr-secret your-api-secret \
  --structurizr-workspace-id 123
```

### 2. Environment Variable
```bash
export DISABLE_SSL_VERIFICATION=true
java -jar structurizr-confluence.jar export [other options...]
```

### 3. System Property
```bash
java -Ddisable.ssl.verification=true -jar structurizr-confluence.jar export [other options...]
```

## What Gets Configured

When SSL verification is disabled, the following components are configured to trust all certificates:

1. **Confluence REST Client** - API calls to Confluence
2. **HttpClient instances** - File uploads and downloads
3. **Structurizr Client** - Workspace loading from Structurizr
4. **Playwright Browser** - Diagram export from Structurizr web UI

## Security Warning

⚠️ **WARNING**: Disabling SSL certificate verification should only be used:
- In development environments
- When connecting to trusted endpoints with self-signed certificates
- Never in production with untrusted certificates

This feature makes your application vulnerable to man-in-the-middle attacks when used inappropriately.

## Example Usage

```bash
# For a complete export with self-signed certificates
java -jar structurizr-confluence.jar export \
  --disable-ssl-verification \
  --confluence-url https://confluence.internal.company.com \
  --confluence-user john.doe@company.com \
  --confluence-token ATATTxxxxx \
  --confluence-space ARCH \
  --structurizr-url https://structurizr.internal.company.com \
  --structurizr-key your-api-key \
  --structurizr-secret your-api-secret \
  --structurizr-workspace-id 42
```