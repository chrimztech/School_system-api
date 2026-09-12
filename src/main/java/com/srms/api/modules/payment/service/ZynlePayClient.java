package com.srms.api.modules.payment.service;

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

/**
 * Thin client for ZynlePay's JSON API. All deposit/disbursement/balance operations share one
 * base URL and are differentiated by "channel" + "data.method"; PaymentStatus alone uses a
 * separate URL and a flat (non auth/data-wrapped) body.
 *
 * Credentials resolve through PlatformIntegrationConfigService first (set from the Developer
 * Console, no redeploy needed) and fall back to the injected @Value defaults (the original
 * env-var-only path) when no enabled override is on file — see resolve()/isConfigured() below.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ZynlePayClient {
    private final PlatformIntegrationConfigService integrationConfig;

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

    private String resolve(java.util.function.Function<com.srms.api.modules.integration.entity.PlatformIntegrationConfig, String> field, String fallback) {
        return integrationConfig.resolve(PlatformIntegrationConfigService.ZYNLEPAY, field, fallback);
    }

    private String baseUrl() { return resolve(c -> c.getBaseUrl(), defaultBaseUrl); }
    private String paymentStatusUrl() { return resolve(c -> c.getSecondaryUrl(), defaultPaymentStatusUrl); }
    private String merchantId() { return resolve(c -> c.getAccountId(), defaultMerchantId); }
    private String apiId() { return resolve(c -> c.getClientId(), defaultApiId); }
    private String apiKey() { return resolve(c -> c.getApiKey(), defaultApiKey); }

    /** No school has ever configured real ZynlePay merchant credentials yet — every deposit,
     * balance, and status call would hit the sandbox with blank auth and fail. Callers check
     * this first so a parent trying to pay fees gets an honest "not available yet" message
     * instead of ZynlePay's raw "Wrong API credentials" response. */
    public boolean isConfigured() {
        return notBlank(merchantId()) && notBlank(apiId()) && notBlank(apiKey());
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> postToGateway(String channel, Map<String, Object> data) {
        Map<String, Object> auth = new LinkedHashMap<>();
        auth.put("merchant_id", merchantId());
        auth.put("api_id", apiId());
        auth.put("api_key", apiKey());
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
            Map<String, Object> response = restTemplate.postForObject(baseUrl(), request, Map.class);
            return response != null ? response : Map.of();
        } catch (Exception e) {
            log.error("ZynlePay gateway call failed (channel={}, method={}): {}", channel, data.get("method"), e.getMessage());
            throw new RuntimeException("Payment gateway is unreachable — please try again shortly", e);
        }
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> checkStatus(String referenceNo) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("api_id", apiId());
        body.put("api_key", apiKey());
        body.put("reference_no", referenceNo);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            Map<String, Object> response = restTemplate.postForObject(paymentStatusUrl(), request, Map.class);
            return response != null ? response : Map.of();
        } catch (Exception e) {
            log.error("ZynlePay status check failed (reference_no={}): {}", referenceNo, e.getMessage());
            throw new RuntimeException("Unable to reach payment gateway for status check", e);
        }
    }
}
