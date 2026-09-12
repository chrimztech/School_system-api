package com.srms.api.modules.integration.client;

import com.srms.api.modules.integration.service.IntegrationConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.Optional;

/**
 * Real client for Google's OpenID Connect identity tokens — backs "Sign in with Google" for
 * staff whose school has connected Google Workspace SSO. Configuration fields exactly match the
 * platform's integration-configuration spec:
 *   configuration: workspaceDomain, googleCloudProjectId, oauthClientId, allowedEmailDomains,
 *                  requestedScopes, enforceDomainRestriction, autoCreateStaffAccounts,
 *                  defaultRoleForNewUsers, requireVerifiedEmail
 *   credentials:   oauthClientSecret (not currently used by the ID-token verification flow below,
 *                  which only needs the public client ID — kept for a future authorization-code
 *                  flow that would need it server-side)
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

    private final IntegrationConfigService config;
    private final RestTemplate restTemplate = new RestTemplate();

    public record VerifiedIdentity(String email, boolean emailVerified) {}

    private String str(Optional<Object> v) { return v.map(String::valueOf).orElse(null); }

    /** Verifies the ID token against Google, then checks its audience matches the Client ID the
     * school actually configured — without that check, an ID token minted for any other Google
     * app in the world would otherwise be accepted here too. Also enforces the configured
     * allowed-email-domain restriction, if the admin turned that on. */
    @SuppressWarnings("unchecked")
    public Optional<VerifiedIdentity> verify(String schoolId, String idToken) {
        String expectedClientId = str(config.resolveConfig(CODE, schoolId, "oauthClientId"));
        if (expectedClientId == null || expectedClientId.isBlank()) return Optional.empty();

        try {
            Map<String, Object> claims = restTemplate.getForObject(TOKENINFO_URL + idToken, Map.class);
            if (claims == null || claims.get("email") == null) return Optional.empty();
            String aud = String.valueOf(claims.get("aud"));
            if (!expectedClientId.equals(aud)) {
                log.warn("Google ID token audience mismatch for school {} (expected {}, got {})", schoolId, expectedClientId, aud);
                return Optional.empty();
            }
            String email = String.valueOf(claims.get("email"));
            boolean enforceDomain = Boolean.parseBoolean(String.valueOf(config.resolveConfig(CODE, schoolId, "enforceDomainRestriction").orElse(false)));
            if (enforceDomain) {
                Object allowedDomains = config.resolveConfig(CODE, schoolId, "allowedEmailDomains").orElse(null);
                if (!emailDomainAllowed(email, allowedDomains)) {
                    log.warn("Google sign-in blocked for school {}: {} is not in an allowed domain", schoolId, email);
                    return Optional.empty();
                }
            }
            boolean verified = Boolean.parseBoolean(String.valueOf(claims.get("email_verified")));
            return Optional.of(new VerifiedIdentity(email, verified));
        } catch (Exception e) {
            log.warn("Google ID token verification failed for school {}: {}", schoolId, e.getMessage());
            return Optional.empty();
        }
    }

    private boolean emailDomainAllowed(String email, Object allowedDomains) {
        if (allowedDomains == null) return true;
        String domain = email.contains("@") ? email.substring(email.indexOf('@') + 1).toLowerCase() : "";
        if (allowedDomains instanceof java.util.List<?> list) {
            return list.stream().anyMatch(d -> String.valueOf(d).equalsIgnoreCase(domain));
        }
        return String.valueOf(allowedDomains).toLowerCase().contains(domain);
    }

    /** "Test connection" for this provider is a reachability + shape check on Google's own
     * OpenID discovery document — there's no per-tenant secret to authenticate with since ID
     * token verification is public, so this at least confirms the configured Client ID is
     * present and Google's endpoint is reachable from this server. */
    public IntegrationTestResult test(String schoolId) {
        String clientId = str(config.resolveConfig(CODE, schoolId, "oauthClientId"));
        if (clientId == null || clientId.isBlank()) {
            return IntegrationTestResult.fail("OAuth 2.0 Client ID is required");
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
}
