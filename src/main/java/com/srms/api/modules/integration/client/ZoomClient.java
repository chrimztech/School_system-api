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
import org.springframework.web.util.UriComponentsBuilder;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Real client for Zoom's Server-to-Server OAuth API (https://developers.zoom.us/docs/internal-apps/s2s-oauth/)
 * — creates real Zoom meetings for virtual classes/staff meetings. Per-school credentials:
 *   - accountId = Zoom Account ID
 *   - clientId, mapped onto baseUrl field = Client ID
 *   - apiKey    = Client Secret
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ZoomClient {
    public static final String CODE = "zoom";
    private static final String TOKEN_URL = "https://zoom.us/oauth/token";
    private static final String API_BASE = "https://api.zoom.us/v2";

    private final IntegrationService integrationService;
    private final RestTemplate restTemplate = new RestTemplate();

    private Optional<String> fetchToken(IntegrationConnection conn) {
        if (isBlank(conn.getAccountId()) || isBlank(conn.getBaseUrl()) || isBlank(conn.getApiKey())) return Optional.empty();
        try {
            String credentials = conn.getBaseUrl() + ":" + conn.getApiKey();
            String basicAuth = java.util.Base64.getEncoder().encodeToString(credentials.getBytes());

            String url = UriComponentsBuilder.fromHttpUrl(TOKEN_URL)
                    .queryParam("grant_type", "account_credentials")
                    .queryParam("account_id", conn.getAccountId())
                    .toUriString();

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Basic " + basicAuth);

            var response = restTemplate.postForEntity(url, new HttpEntity<>(headers), Map.class);
            Object token = response.getBody() != null ? response.getBody().get("access_token") : null;
            return Optional.ofNullable(token).map(String::valueOf);
        } catch (Exception e) {
            log.warn("Zoom token fetch failed: {}", e.getMessage());
            return Optional.empty();
        }
    }

    public IntegrationTestResult test(String schoolId) {
        Optional<IntegrationConnection> connOpt = integrationService.getConnected(schoolId, CODE);
        if (connOpt.isEmpty()) return IntegrationTestResult.fail("Not connected");
        if (isBlank(connOpt.get().getAccountId()) || isBlank(connOpt.get().getBaseUrl()) || isBlank(connOpt.get().getApiKey())) {
            return IntegrationTestResult.fail("Account ID, Client ID (Base URL field), and Client Secret (API key) are all required");
        }
        Optional<String> token = fetchToken(connOpt.get());
        return token.isPresent()
                ? IntegrationTestResult.ok("Connected — Zoom Server-to-Server OAuth token issued")
                : IntegrationTestResult.fail("Could not authenticate with Zoom — check the Account ID, Client ID, and Client Secret");
    }

    /** Creates a real, scheduled Zoom meeting and returns its join URL — used for a genuine
     * "Create meeting" action rather than a simulated one. */
    public Optional<String> createMeeting(String schoolId, String topic, String startTimeIso) {
        Optional<IntegrationConnection> connOpt = integrationService.getConnected(schoolId, CODE);
        if (connOpt.isEmpty()) return Optional.empty();
        Optional<String> token = fetchToken(connOpt.get());
        if (token.isEmpty()) return Optional.empty();

        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("topic", topic);
            body.put("type", startTimeIso != null ? 2 : 1);
            if (startTimeIso != null) body.put("start_time", startTimeIso);
            body.put("settings", Map.of("join_before_host", true, "waiting_room", false));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(token.get());

            var response = restTemplate.postForEntity(API_BASE + "/users/me/meetings", new HttpEntity<>(body, headers), Map.class);
            Object joinUrl = response.getBody() != null ? response.getBody().get("join_url") : null;
            return Optional.ofNullable(joinUrl).map(String::valueOf);
        } catch (Exception e) {
            log.warn("Zoom create meeting failed for school {}: {}", schoolId, e.getMessage());
            return Optional.empty();
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
