package com.srms.api.modules.payment.service;

import com.srms.api.modules.integration.entity.IntegrationConnection;
import com.srms.api.modules.integration.service.IntegrationService;
import com.srms.api.modules.integration.service.PlatformIntegrationConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Thin client for ZynlePay's JSON API. All deposit/disbursement/balance operations share one
 * base URL and are differentiated by "channel" + "data.method"; PaymentStatus alone uses a
 * separate URL and a flat (non auth/data-wrapped) body.
 *
 * Two independent, deliberately separate credential scopes:
 *   - Per-school (IntegrationConnection, code "zynlepay", configured on that school's own
 *     Integrations page): used for every parent-facing fee payment, so money settles into that
 *     school's own merchant account, not the platform's. This is what postToGateway(schoolId,...)/
 *     checkStatus(schoolId,...)/isConfigured(schoolId) resolve, falling back to the platform-level
 *     scope below only if a school hasn't connected its own account yet.
 *   - Platform-level (PlatformIntegrationConfigService, configured from the Developer Console):
 *     the platform's own merchant account — used by getMerchantBalance() (platform admin checking
 *     their own balance) and as the fallback above. This is also the account any future
 *     school-pays-platform subscription billing would use, since that revenue is genuinely the
 *     platform's. Falls back to the original @Value env-var defaults when nothing is configured.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ZynlePayClient {
    public static final String CODE = "zynlepay";

    private final PlatformIntegrationConfigService integrationConfig;
    private final IntegrationService schoolIntegrationConfig;

    @Value("${zynlepay.baseUrl}")
    private String defaultBaseUrl;

    @Value("${zynlepay.paymentStatusUrl}")
    private String defaultPaymentStatusUrl;

    @Value("${zynlepay.merchantId}")
    private String defaultMerchantId;

    @Value("${zynlepay.apiId}")
    private String defaultApiId;

    @Value("${zynlepay.apiKey}")
    private String defaultApiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    // ---- Platform-level resolution — the platform's own merchant account ----

    private String platformResolve(Function<com.srms.api.modules.integration.entity.PlatformIntegrationConfig, String> field, String fallback) {
        return integrationConfig.resolve(PlatformIntegrationConfigService.ZYNLEPAY, field, fallback);
    }

    private String platformBaseUrl() { return platformResolve(c -> c.getBaseUrl(), defaultBaseUrl); }
    private String platformPaymentStatusUrl() { return platformResolve(c -> c.getSecondaryUrl(), defaultPaymentStatusUrl); }
    private String platformMerchantId() { return platformResolve(c -> c.getAccountId(), defaultMerchantId); }
    private String platformApiId() { return platformResolve(c -> c.getClientId(), defaultApiId); }
    private String platformApiKey() { return platformResolve(c -> c.getApiKey(), defaultApiKey); }

    /** Used by getMerchantBalance() only — the platform admin checking their own account. */
    public boolean isConfigured() {
        return notBlank(platformMerchantId()) && notBlank(platformApiId()) && notBlank(platformApiKey());
    }

    // ---- Per-school resolution — falls back to the platform scope above ----

    private String schoolResolve(String schoolId, Function<IntegrationConnection, String> field, Supplier<String> platformFallback) {
        Optional<IntegrationConnection> conn = schoolIntegrationConfig.getConnected(schoolId, CODE);
        if (conn.isPresent()) {
            String value = field.apply(conn.get());
            if (notBlank(value)) return value;
        }
        return platformFallback.get();
    }

    private String baseUrlFor(String schoolId) { return schoolResolve(schoolId, IntegrationConnection::getBaseUrl, this::platformBaseUrl); }
    private String paymentStatusUrlFor(String schoolId) { return schoolResolve(schoolId, IntegrationConnection::getSecondaryUrl, this::platformPaymentStatusUrl); }
    private String merchantIdFor(String schoolId) { return schoolResolve(schoolId, IntegrationConnection::getAccountId, this::platformMerchantId); }
    // ZynlePay's "API ID" isn't quite as sensitive as the API key, but there's no dedicated plain
    // field for it on IntegrationConnection — apiSecret (encrypted) is the closest fit and the
    // extra protection doesn't hurt.
    private String apiIdFor(String schoolId) { return schoolResolve(schoolId, IntegrationConnection::getApiSecret, this::platformApiId); }
    private String apiKeyFor(String schoolId) { return schoolResolve(schoolId, IntegrationConnection::getApiKey, this::platformApiKey); }

    /** Callers check this first so a parent trying to pay fees gets an honest "not available yet"
     * message instead of ZynlePay's raw "Wrong API credentials" response. */
    public boolean isConfigured(String schoolId) {
        return notBlank(merchantIdFor(schoolId)) && notBlank(apiIdFor(schoolId)) && notBlank(apiKeyFor(schoolId));
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    /** Platform-level gateway call — used only by getMerchantBalance(). */
    public Map<String, Object> postToGateway(String channel, Map<String, Object> data) {
        return doPost(platformBaseUrl(), platformMerchantId(), platformApiId(), platformApiKey(), channel, data);
    }

    /** Per-school gateway call — used for every parent-facing fee payment. */
    public Map<String, Object> postToGateway(String schoolId, String channel, Map<String, Object> data) {
        return doPost(baseUrlFor(schoolId), merchantIdFor(schoolId), apiIdFor(schoolId), apiKeyFor(schoolId), channel, data);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> doPost(String baseUrl, String merchantId, String apiId, String apiKey, String channel, Map<String, Object> data) {
        Map<String, Object> auth = new LinkedHashMap<>();
        auth.put("merchant_id", merchantId);
        auth.put("api_id", apiId);
        auth.put("api_key", apiKey);
        if (channel != null) {
            auth.put("channel", channel);
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("auth", auth);
        body.put("data", data);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            Map<String, Object> response = restTemplate.postForObject(baseUrl, request, Map.class);
            return response != null ? response : Map.of();
        } catch (Exception e) {
            log.error("ZynlePay gateway call failed (channel={}, method={}): {}", channel, data.get("method"), e.getMessage());
            throw new RuntimeException("Payment gateway is unreachable — please try again shortly", e);
        }
    }

    /** Per-school status check — used for every parent-facing fee payment. */
    @SuppressWarnings("unchecked")
    public Map<String, Object> checkStatus(String schoolId, String referenceNo) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("api_id", apiIdFor(schoolId));
        body.put("api_key", apiKeyFor(schoolId));
        body.put("reference_no", referenceNo);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            Map<String, Object> response = restTemplate.postForObject(paymentStatusUrlFor(schoolId), request, Map.class);
            return response != null ? response : Map.of();
        } catch (Exception e) {
            log.error("ZynlePay status check failed (school={}, reference_no={}): {}", schoolId, referenceNo, e.getMessage());
            throw new RuntimeException("Unable to reach payment gateway for status check", e);
        }
    }
}
