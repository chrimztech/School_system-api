package com.srms.api.modules.integration.client;

import com.srms.api.modules.integration.entity.IntegrationConnection;
import com.srms.api.modules.integration.service.IntegrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Client for the Examinations Council of Zambia's candidate registration/results system.
 *
 * IMPORTANT — unlike the other clients in this package, ECZ does not publish a public API
 * specification. There is no way to build or verify a client against ECZ's real system without
 * that documentation (which typically requires a data-sharing agreement between ECZ and the
 * school/vendor). This client therefore makes real outbound HTTP calls — Bearer-token
 * authenticated JSON POST/GET, the most common shape for this kind of integration — to whatever
 * base URL the school configures, so it is genuinely live once pointed at a real ECZ endpoint. But
 * the request/response field names below (candidates, examCenterNumber, results) are this
 * project's best-effort guess at the described use case ("Candidate registration and results
 * download"), not a contract confirmed against ECZ. Treat this as the wiring to finish once ECZ's
 * actual API contract is available, not as a verified integration.
 *
 * Per-school config: baseUrl = ECZ endpoint root, accountId = examination center number,
 * apiKey = Bearer token issued by ECZ for that center.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EczSyncClient {
    public static final String CODE = "ecz";

    private final IntegrationService integrationService;
    private final RestTemplate restTemplate = new RestTemplate();

    public IntegrationTestResult test(String schoolId) {
        Optional<IntegrationConnection> connOpt = integrationService.getConnected(schoolId, CODE);
        if (connOpt.isEmpty()) return IntegrationTestResult.fail("Not connected");
        IntegrationConnection conn = connOpt.get();
        if (isBlank(conn.getBaseUrl()) || isBlank(conn.getAccountId()) || isBlank(conn.getApiKey())) {
            return IntegrationTestResult.fail("Endpoint (Base URL), Examination center number (Account ID), and Bearer token (API key) are all required");
        }
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(conn.getApiKey());
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));

            String url = conn.getBaseUrl().replaceAll("/+$", "") + "/centers/" + conn.getAccountId();
            var response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), String.class);
            return response.getStatusCode().is2xxSuccessful()
                    ? IntegrationTestResult.ok("Endpoint reachable and accepted the configured credentials")
                    : IntegrationTestResult.fail("Endpoint responded with " + response.getStatusCode());
        } catch (Exception e) {
            log.warn("ECZ sync test failed for school {}: {}", schoolId, e.getMessage());
            return IntegrationTestResult.fail("Could not reach the configured ECZ endpoint: " + shortMessage(e)
                    + " — verify the endpoint URL and token with ECZ, this contract is not publicly documented");
        }
    }

    /** Best-effort candidate registration push — see the class-level note on why this contract is
     * provisional. Returns the raw response body on success so an admin can see exactly what ECZ
     * (or whatever endpoint is configured) actually said, rather than a fabricated confirmation. */
    public Optional<String> syncCandidates(String schoolId, Object candidatesPayload) {
        Optional<IntegrationConnection> connOpt = integrationService.getConnected(schoolId, CODE);
        if (connOpt.isEmpty()) return Optional.empty();
        IntegrationConnection conn = connOpt.get();
        if (isBlank(conn.getBaseUrl()) || isBlank(conn.getApiKey())) return Optional.empty();

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(conn.getApiKey());
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> body = Map.of(
                    "examCenterNumber", conn.getAccountId(),
                    "candidates", candidatesPayload
            );

            String url = conn.getBaseUrl().replaceAll("/+$", "") + "/candidates/sync";
            var response = restTemplate.postForEntity(url, new HttpEntity<>(body, headers), String.class);
            return Optional.ofNullable(response.getBody());
        } catch (Exception e) {
            log.warn("ECZ candidate sync failed for school {}: {}", schoolId, e.getMessage());
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
