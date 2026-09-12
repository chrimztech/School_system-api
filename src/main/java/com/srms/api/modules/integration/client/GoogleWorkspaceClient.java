package com.srms.api.modules.integration.client;

import com.srms.api.modules.integration.entity.IntegrationConnection;
import com.srms.api.modules.integration.service.IntegrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.Optional;

/**
 * Real client for Google's OpenID Connect identity tokens — backs "Sign in with Google" for
 * staff whose school has connected Google Workspace. Per-school config: accountId = OAuth 2.0
 * Client ID (from Google Cloud Console), created for this app's frontend origin.
 *
 * Verification uses Google's public tokeninfo endpoint (https://developers.google.com/identity/openid-connect/openid-connect#validatinganidtoken)
 * — a real network call to Google, not a local JWT-signature check — which is the simplest
 * correct way to validate an ID token without adding a new dependency for JWKS handling.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleWorkspaceClient {
    public static final String CODE = "google";
    private static final String TOKENINFO_URL = "https://oauth2.googleapis.com/tokeninfo?id_token=";

    private final IntegrationService integrationService;
    private final RestTemplate restTemplate = new RestTemplate();

    public record VerifiedIdentity(String email, boolean emailVerified) {}

    /** Verifies the ID token against Google, then checks its audience matches the Client ID the
     * school actually configured — without that check, an ID token minted for any other Google
     * app in the world would otherwise be accepted here too. */
    @SuppressWarnings("unchecked")
    public Optional<VerifiedIdentity> verify(String schoolId, String idToken) {
        Optional<IntegrationConnection> connOpt = integrationService.getConnected(schoolId, CODE);
        if (connOpt.isEmpty() || isBlank(connOpt.get().getAccountId())) return Optional.empty();
        String expectedClientId = connOpt.get().getAccountId();

        try {
            Map<String, Object> claims = restTemplate.getForObject(TOKENINFO_URL + idToken, Map.class);
            if (claims == null || claims.get("email") == null) return Optional.empty();
            String aud = String.valueOf(claims.get("aud"));
            if (!expectedClientId.equals(aud)) {
                log.warn("Google ID token audience mismatch for school {} (expected {}, got {})", schoolId, expectedClientId, aud);
                return Optional.empty();
            }
            boolean verified = Boolean.parseBoolean(String.valueOf(claims.get("email_verified")));
            return Optional.of(new VerifiedIdentity(String.valueOf(claims.get("email")), verified));
        } catch (Exception e) {
            log.warn("Google ID token verification failed for school {}: {}", schoolId, e.getMessage());
            return Optional.empty();
        }
    }

    /** "Test connection" for this provider is a reachability + shape check on Google's own
     * OpenID discovery document — there's no per-tenant secret to authenticate with since ID
     * token verification is public, so this at least confirms the configured Client ID is
     * present and Google's endpoint is reachable from this server. */
    public IntegrationTestResult test(String schoolId) {
        Optional<IntegrationConnection> connOpt = integrationService.getConnected(schoolId, CODE);
        if (connOpt.isEmpty()) return IntegrationTestResult.fail("Not connected");
        if (isBlank(connOpt.get().getAccountId())) {
            return IntegrationTestResult.fail("OAuth 2.0 Client ID (Account/merchant ID) is required");
        }
        try {
            Map<?, ?> discovery = restTemplate.getForObject("https://accounts.google.com/.well-known/openid-configuration", Map.class);
            return discovery != null && discovery.get("issuer") != null
                    ? IntegrationTestResult.ok("Client ID configured — Google's identity endpoint is reachable")
                    : IntegrationTestResult.fail("Could not read Google's OpenID configuration");
        } catch (Exception e) {
            log.warn("Google Workspace test failed for school {}: {}", schoolId, e.getMessage());
            return IntegrationTestResult.fail("Could not reach Google: " + e.getMessage());
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
