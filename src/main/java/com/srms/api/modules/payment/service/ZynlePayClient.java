package com.srms.api.modules.payment.service;

import com.srms.api.modules.integration.service.IntegrationConfigService;
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

/**
 * Thin client for ZynlePay's JSON API. All deposit/disbursement/balance operations share one
 * base URL and are differentiated by "channel" + "data.method"; PaymentStatus alone uses a
 * separate URL and a flat (non auth/data-wrapped) body.
 *
 * Configuration fields (IntegrationConfigService, provider code "zynlepay"):
 *   configuration: environment, merchantId, apiBaseUrl, paymentStatusUrl
 *   credentials:   apiId, apiKey
 *
 * Two independent, deliberately separate scopes:
 *   - SCHOOL: configured on that school's own Integrations page — used for every parent-facing
 *     fee payment, so money settles into that school's own merchant account, not the platform's.
 *   - PLATFORM: configured from the Developer Console — the platform's own merchant account,
 *     used by getMerchantBalance() (platform admin checking their own balance), as the fallback
 *     for a school that hasn't connected its own account yet, and for any future
 *     school-pays-platform subscription billing.
 * Falls back to the original @Value env-var defaults when nothing is configured at either scope,
 * preserving the very first (pre-database) configuration path.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ZynlePayClient {
    public static final String CODE = "zynlepay";

    private final IntegrationConfigService config;

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

    private String str(Optional<Object> v) { return v.map(String::valueOf).orElse(null); }

    // ---- Platform-level resolution — the platform's own merchant account ----

    private String platformConfig(String key, String fallback) {
        String v = str(config.resolveConfig(CODE, com.srms.api.modules.integration.entity.IntegrationConfig.PLATFORM_SCOPE_SCHOOL_ID, key));
        return notBlank(v) ? v : fallback;
    }

    private String platformCredential(String key, String fallback) {
        return config.resolveCredential(CODE, com.srms.api.modules.integration.entity.IntegrationConfig.PLATFORM_SCOPE_SCHOOL_ID, key).filter(ZynlePayClient::notBlank).orElse(fallback);
    }

    private String platformBaseUrl() { return platformConfig("apiBaseUrl", defaultBaseUrl); }
    private String platformPaymentStatusUrl() { return platformConfig("paymentStatusUrl", defaultPaymentStatusUrl); }
    private String platformMerchantId() { return platformConfig("merchantId", defaultMerchantId); }
    private String platformApiId() { return platformCredential("apiId", defaultApiId); }
    private String platformApiKey() { return platformCredential("apiKey", defaultApiKey); }

    /** Used by getMerchantBalance() only — the platform admin checking their own account. */
    public boolean isConfigured() {
        return notBlank(platformMerchantId()) && notBlank(platformApiId()) && notBlank(platformApiKey());
    }

    // ---- Per-school resolution — falls back to the platform scope above ----

    private String baseUrlFor(String schoolId) {
        return config.resolveConfig(CODE, schoolId, "apiBaseUrl").map(String::valueOf).filter(ZynlePayClient::notBlank).orElseGet(this::platformBaseUrl);
    }

    private String paymentStatusUrlFor(String schoolId) {
        return config.resolveConfig(CODE, schoolId, "paymentStatusUrl").map(String::valueOf).filter(ZynlePayClient::notBlank).orElseGet(this::platformPaymentStatusUrl);
    }

    private String merchantIdFor(String schoolId) {
        return config.resolveConfig(CODE, schoolId, "merchantId").map(String::valueOf).filter(ZynlePayClient::notBlank).orElseGet(this::platformMerchantId);
    }

    private String apiIdFor(String schoolId) {
        return config.resolveCredential(CODE, schoolId, "apiId").filter(ZynlePayClient::notBlank).orElseGet(this::platformApiId);
    }

    private String apiKeyFor(String schoolId) {
        return config.resolveCredential(CODE, schoolId, "apiKey").filter(ZynlePayClient::notBlank).orElseGet(this::platformApiKey);
    }

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
