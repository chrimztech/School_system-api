package com.srms.api.modules.integration.dto;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Read-side shape for an IntegrationConfig row. Every credential is masked generically: whatever
 * keys exist inside the encrypted credentials JSON are reported as {@code credentials}, each
 * either a masked hint (last 4 characters) or null if never set — plaintext values are never
 * present here, not even right after the admin who just typed them saves the form.
 */
public record IntegrationConfigView(
        String id,
        String scopeType,
        String schoolId,
        String providerCode,
        String displayName,
        String category,
        String environment,
        Boolean enabled,
        String connectionStatus,
        Map<String, Object> configuration,
        Map<String, String> credentials,
        String callbackUrl,
        LocalDateTime lastTestedAt,
        LocalDateTime lastSuccessAt,
        LocalDateTime lastFailureAt,
        String lastErrorMessage,
        String createdBy,
        LocalDateTime createdAt,
        String updatedBy,
        LocalDateTime updatedAt
) {
}
