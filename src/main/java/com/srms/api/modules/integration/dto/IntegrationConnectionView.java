package com.srms.api.modules.integration.dto;

import com.srms.api.modules.integration.entity.IntegrationConnection;

/**
 * Read-side shape for an integration connection — the entity's {@code apiKey}/{@code apiSecret}
 * are encrypted at rest and must never be echoed back to the frontend in plaintext, not even
 * right after the admin who just typed them saves the form. This carries only a presence flag
 * and a masked hint (last 4 characters) instead, matching how Stripe/AWS/etc. show a saved key
 * back to you afterward.
 */
public record IntegrationConnectionView(
        String id,
        String code,
        String name,
        String category,
        String description,
        Boolean connected,
        String status,
        String owner,
        String webhook,
        String accountId,
        String baseUrl,
        String environment,
        boolean hasApiKey,
        String apiKeyMasked,
        boolean hasApiSecret,
        String apiSecretMasked
) {
    public static IntegrationConnectionView from(IntegrationConnection c) {
        return new IntegrationConnectionView(
                c.getId(), c.getCode(), c.getName(), c.getCategory(), c.getDescription(),
                c.getConnected(), c.getStatus(), c.getOwner(), c.getWebhook(),
                c.getAccountId(), c.getBaseUrl(), c.getEnvironment(),
                hasValue(c.getApiKey()), mask(c.getApiKey()),
                hasValue(c.getApiSecret()), mask(c.getApiSecret())
        );
    }

    private static boolean hasValue(String s) {
        return s != null && !s.isBlank();
    }

    private static String mask(String secret) {
        if (!hasValue(secret)) return null;
        int keep = Math.min(4, secret.length());
        return "••••" + secret.substring(secret.length() - keep);
    }
}
