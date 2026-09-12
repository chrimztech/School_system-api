package com.srms.api.modules.integration.dto;

import lombok.Data;

/**
 * Request/response shape for platform-level integration config. On a read, apiKey always comes
 * back as a masked hint (last 4 characters only, or null if never set) — never the real secret,
 * same convention as IntegrationConnectionView. On a write, apiKey is only changed when a
 * non-blank value is actually sent — this lets the frontend save every other field without
 * accidentally clobbering an already-stored key with a masked hint round-tripped back to us.
 */
@Data
public class PlatformIntegrationConfigDto {
    private String provider;
    private Boolean enabled;
    private String baseUrl;
    private String secondaryUrl;
    private String accountId;
    private String clientId;
    private String apiKey;
    private boolean apiKeySet;
}
