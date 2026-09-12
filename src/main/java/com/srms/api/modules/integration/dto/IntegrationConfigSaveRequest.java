package com.srms.api.modules.integration.dto;

import java.util.Map;

/**
 * Write-side shape for saving an IntegrationConfig. {@code configuration} is replaced wholesale
 * (the frontend always submits its full non-secret form state). {@code credentials} follows the
 * established "blank/absent means don't change" convention — a key with a non-blank value
 * overwrites that credential; a blank or missing key leaves whatever is already stored untouched,
 * so reopening the form and saving an unrelated field can never accidentally wipe a secret.
 */
public record IntegrationConfigSaveRequest(
        String displayName,
        String category,
        String environment,
        Boolean enabled,
        Map<String, Object> configuration,
        Map<String, String> credentials
) {
}
