package com.srms.api.modules.integration.client;

import com.srms.api.modules.integration.entity.IntegrationConnection;
import com.srms.api.modules.integration.service.IntegrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.Optional;

/**
 * Real client for Africa's Talking's SMS API (https://developers.africastalking.com/docs/sms/overview).
 * Per-school credentials: accountId = "username" (the AT application username), apiKey = the AT API key.
 * baseUrl selects sandbox vs. production ("https://api.sandbox.africastalking.com" vs
 * "https://api.africastalking.com") — defaults to production if left blank.
 *
 * This is a school-level alternative SMS sender, separate from the platform's existing Zamtel
 * BulkSMS path (ZamtelSmsClient) — schools that connect this here can use it for parent/guardian
 * notifications; schools that don't keep using Zamtel exactly as before.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AfricasTalkingSmsClient {
    public static final String CODE = "sms";
    private static final String PROD_BASE = "https://api.africastalking.com";

    private final IntegrationService integrationService;
    private final RestTemplate restTemplate = new RestTemplate();

    /** Safe, free, read-only call (fetch account balance) used both to verify credentials are
     * genuinely valid and as the "Test connection" action on the Integrations page. */
    public IntegrationTestResult test(String schoolId) {
        Optional<IntegrationConnection> connOpt = integrationService.getConnected(schoolId, CODE);
        if (connOpt.isEmpty()) return IntegrationTestResult.fail("Not connected");
        IntegrationConnection conn = connOpt.get();
        if (isBlank(conn.getAccountId()) || isBlank(conn.getApiKey())) {
            return IntegrationTestResult.fail("Username and API key are both required");
        }
        try {
            String base = isBlank(conn.getBaseUrl()) ? PROD_BASE : conn.getBaseUrl();
            String url = base + "/version1/user?username=" + conn.getAccountId();
            HttpHeaders headers = new HttpHeaders();
            headers.set("apiKey", conn.getApiKey());
            headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));
            var response = restTemplate.exchange(url, org.springframework.http.HttpMethod.GET,
                    new HttpEntity<>(headers), Map.class);
            Object balance = response.getBody() != null
                    ? ((Map<?, ?>) response.getBody().getOrDefault("UserData", Map.of())).get("balance")
                    : null;
            return IntegrationTestResult.ok(balance != null
                    ? "Connected — account balance: " + balance
                    : "Connected — account reachable");
        } catch (Exception e) {
            log.warn("Africa's Talking test failed for school {}: {}", schoolId, e.getMessage());
            return IntegrationTestResult.fail("Could not reach Africa's Talking: " + shortMessage(e));
        }
    }

    /** Real SMS send, used by NotificationService when a school has this provider connected. */
    @SuppressWarnings("unchecked")
    public boolean sendSms(String schoolId, String to, String message) {
        Optional<IntegrationConnection> connOpt = integrationService.getConnected(schoolId, CODE);
        if (connOpt.isEmpty()) return false;
        IntegrationConnection conn = connOpt.get();
        if (isBlank(conn.getAccountId()) || isBlank(conn.getApiKey()) || isBlank(to)) return false;
        try {
            String base = isBlank(conn.getBaseUrl()) ? PROD_BASE : conn.getBaseUrl();
            String url = base + "/version1/messaging";

            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("username", conn.getAccountId());
            form.add("to", to);
            form.add("message", message);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.set("apiKey", conn.getApiKey());
            headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));

            var response = restTemplate.postForEntity(url, new HttpEntity<>(form, headers), Map.class);
            Map<String, Object> body = response.getBody();
            if (body == null) return false;
            Map<String, Object> smsData = (Map<String, Object>) body.get("SMSMessageData");
            return smsData != null && String.valueOf(smsData.get("Message")).toLowerCase().contains("sent");
        } catch (Exception e) {
            log.warn("Africa's Talking send failed for school {}: {}", schoolId, e.getMessage());
            return false;
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
