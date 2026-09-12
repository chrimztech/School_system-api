package com.srms.api.modules.integration.client;

import com.srms.api.modules.integration.entity.IntegrationConnection;
import com.srms.api.modules.integration.service.IntegrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.Optional;

/**
 * Real client for MTN's Mobile Money Open API — Collections product
 * (https://momodeveloper.mtn.com, product "Collections"). Per-school credentials, mapped onto
 * the generic IntegrationConnection fields:
 *   - accountId  = API User ID (a UUID MTN issues when the merchant registers a subscription)
 *   - apiKey     = API Key (generated for that API User)
 *   - apiSecret  = Ocp-Apim-Subscription-Key (the Collections product's primary/secondary key)
 *   - baseUrl    = "https://sandbox.momodeveloper.mtn.com" or the live MTN base URL for the school's market
 *
 * Scope, deliberately: this client only proves the credentials work (fetches a real OAuth2
 * access token, the same handshake a real RequestToPay call would need) — see test(). Wiring an
 * actual RequestToPay collection into the fee-payment settlement flow (status polling,
 * reconciliation, receipts) alongside the existing ZynlePay path is real follow-up work that
 * deserves its own testing against a live MTN sandbox account, which wasn't available here.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MtnMomoClient {
    public static final String CODE = "momo";

    private final IntegrationService integrationService;
    private final RestTemplate restTemplate = new RestTemplate();

    public IntegrationTestResult test(String schoolId) {
        Optional<IntegrationConnection> connOpt = integrationService.getConnected(schoolId, CODE);
        if (connOpt.isEmpty()) return IntegrationTestResult.fail("Not connected");
        IntegrationConnection conn = connOpt.get();
        if (isBlank(conn.getAccountId()) || isBlank(conn.getApiKey()) || isBlank(conn.getApiSecret())) {
            return IntegrationTestResult.fail("API User ID, API Key, and Subscription Key are all required");
        }
        String base = isBlank(conn.getBaseUrl()) ? "https://sandbox.momodeveloper.mtn.com" : conn.getBaseUrl();
        try {
            String credentials = conn.getAccountId() + ":" + conn.getApiKey();
            String basicAuth = java.util.Base64.getEncoder().encodeToString(credentials.getBytes());

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Basic " + basicAuth);
            headers.set("Ocp-Apim-Subscription-Key", conn.getApiSecret());
            headers.setContentLength(0);

            var response = restTemplate.postForEntity(base + "/collection/token/", new HttpEntity<>(headers), Map.class);
            Map<?, ?> body = response.getBody();
            if (body != null && body.get("access_token") != null) {
                return IntegrationTestResult.ok("Connected — OAuth token issued, expires in " + body.get("expires_in") + "s");
            }
            return IntegrationTestResult.fail("MTN did not return an access token");
        } catch (Exception e) {
            log.warn("MTN MoMo test failed for school {}: {}", schoolId, e.getMessage());
            return IntegrationTestResult.fail("Could not authenticate with MTN MoMo: " + shortMessage(e));
        }
    }

    /** One-off token fetch reused by a future RequestToPay call — kept here so that follow-up
     * work doesn't have to duplicate the OAuth handshake above. */
    public Optional<String> fetchAccessToken(IntegrationConnection conn) {
        String base = isBlank(conn.getBaseUrl()) ? "https://sandbox.momodeveloper.mtn.com" : conn.getBaseUrl();
        try {
            String credentials = conn.getAccountId() + ":" + conn.getApiKey();
            String basicAuth = java.util.Base64.getEncoder().encodeToString(credentials.getBytes());
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Basic " + basicAuth);
            headers.set("Ocp-Apim-Subscription-Key", conn.getApiSecret());
            headers.setContentLength(0);
            var response = restTemplate.postForEntity(base + "/collection/token/", new HttpEntity<>(headers), Map.class);
            Object token = response.getBody() != null ? response.getBody().get("access_token") : null;
            return Optional.ofNullable(token).map(String::valueOf);
        } catch (Exception e) {
            log.warn("MTN MoMo token fetch failed: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static String shortMessage(Exception e) {
        String m = e.getMessage();
        return m != null && m.length() > 200 ? m.substring(0, 200) : m;
    }
}
