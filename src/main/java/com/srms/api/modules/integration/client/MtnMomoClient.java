package com.srms.api.modules.integration.client;

import com.srms.api.modules.integration.service.IntegrationConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;
import java.util.Map;
import java.util.Optional;

/**
 * Real client for MTN's Mobile Money Open API — Collections product
 * (https://momoapi.mtn.com/get-started). Configuration fields exactly match the platform's
 * integration-configuration spec for MTN Mobile Money:
 *   configuration: environment, country, currency, merchantName, merchantAccountNumber,
 *                  apiBaseUrl, paymentPrefix, paymentTimeoutSeconds
 *   credentials:   subscriptionKey, apiUserId, apiKey, collectionSubscriptionKey,
 *                  disbursementSubscriptionKey (payouts only), callbackAuthSecret
 *
 * Scope, deliberately: this client proves the credentials work (fetches a real OAuth2 access
 * token, the same handshake a real RequestToPay call would need) — see test(). Wiring an actual
 * RequestToPay collection into the fee-payment settlement flow (status polling, reconciliation,
 * receipts) alongside the existing ZynlePay path is real follow-up work that deserves its own
 * testing against a live MTN sandbox account, which wasn't available here.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MtnMomoClient {
    public static final String CODE = "momo";

    private final IntegrationConfigService config;
    private final RestTemplate restTemplate = new RestTemplate();

    private String str(Optional<Object> v) { return v.map(String::valueOf).orElse(null); }

    public IntegrationTestResult test(String schoolId) {
        String apiUserId = config.resolveCredential(CODE, schoolId, "apiUserId").orElse(null);
        String apiKey = config.resolveCredential(CODE, schoolId, "apiKey").orElse(null);
        String subscriptionKey = config.resolveCredential(CODE, schoolId, "collectionSubscriptionKey")
                .or(() -> config.resolveCredential(CODE, schoolId, "subscriptionKey")).orElse(null);
        String baseUrl = str(config.resolveConfig(CODE, schoolId, "apiBaseUrl"));

        if (isBlank(apiUserId) || isBlank(apiKey) || isBlank(subscriptionKey)) {
            return IntegrationTestResult.fail("API user ID, API key, and a subscription key are all required");
        }
        String effectiveBaseUrl = isBlank(baseUrl) ? "https://sandbox.momodeveloper.mtn.com" : baseUrl;

        try {
            String credentials = apiUserId + ":" + apiKey;
            String basicAuth = Base64.getEncoder().encodeToString(credentials.getBytes());

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Basic " + basicAuth);
            headers.set("Ocp-Apim-Subscription-Key", subscriptionKey);
            headers.setContentLength(0);

            var response = restTemplate.postForEntity(effectiveBaseUrl + "/collection/token/", new HttpEntity<>(headers), Map.class);
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

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static String shortMessage(Exception e) {
        String m = e.getMessage();
        return m != null && m.length() > 200 ? m.substring(0, 200) : m;
    }
}
