package com.srms.api.modules.integration.client;

import com.srms.api.modules.integration.entity.IntegrationConnection;
import com.srms.api.modules.integration.service.IntegrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Real client for the Airtel Money Africa Open API (https://developers.airtel.africa),
 * Collections/Merchant Payments product. Per-school credentials:
 *   - accountId = Client ID
 *   - apiKey    = Client Secret
 *   - baseUrl   = "https://openapiuat.airtel.africa" (sandbox) or the live host for the school's market
 *
 * Same deliberate scope note as MtnMomoClient: test() performs the real OAuth2 client-credentials
 * handshake so "Test connection" reflects genuine, working credentials. Wiring an actual
 * collection request into the fee-payment settlement flow is real follow-up work.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AirtelMoneyClient {
    public static final String CODE = "airtel";

    private final IntegrationService integrationService;
    private final RestTemplate restTemplate = new RestTemplate();

    public IntegrationTestResult test(String schoolId) {
        Optional<IntegrationConnection> connOpt = integrationService.getConnected(schoolId, CODE);
        if (connOpt.isEmpty()) return IntegrationTestResult.fail("Not connected");
        IntegrationConnection conn = connOpt.get();
        if (isBlank(conn.getAccountId()) || isBlank(conn.getApiKey())) {
            return IntegrationTestResult.fail("Client ID and Client Secret are both required");
        }
        String base = isBlank(conn.getBaseUrl()) ? "https://openapiuat.airtel.africa" : conn.getBaseUrl();
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("client_id", conn.getAccountId());
            body.put("client_secret", conn.getApiKey());
            body.put("grant_type", "client_credentials");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            var response = restTemplate.postForEntity(base + "/auth/oauth2/token", new HttpEntity<>(body, headers), Map.class);
            Map<?, ?> respBody = response.getBody();
            if (respBody != null && respBody.get("access_token") != null) {
                return IntegrationTestResult.ok("Connected — OAuth token issued, expires in " + respBody.get("expires_in") + "s");
            }
            return IntegrationTestResult.fail("Airtel did not return an access token");
        } catch (Exception e) {
            log.warn("Airtel Money test failed for school {}: {}", schoolId, e.getMessage());
            return IntegrationTestResult.fail("Could not authenticate with Airtel Money: " + shortMessage(e));
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
