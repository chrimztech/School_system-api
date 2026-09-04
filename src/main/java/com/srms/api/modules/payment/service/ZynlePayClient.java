package com.srms.api.modules.payment.service;

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
 */
@Slf4j
@Service
public class ZynlePayClient {

    @Value("${zynlepay.baseUrl}")
    private String baseUrl;

    @Value("${zynlepay.paymentStatusUrl}")
    private String paymentStatusUrl;

    @Value("${zynlepay.merchantId}")
    private String merchantId;

    @Value("${zynlepay.apiId}")
    private String apiId;

    @Value("${zynlepay.apiKey}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    /** No school has ever configured real ZynlePay merchant credentials yet — every deposit,
     * balance, and status call would hit the sandbox with blank auth and fail. Callers check
     * this first so a parent trying to pay fees gets an honest "not available yet" message
     * instead of ZynlePay's raw "Wrong API credentials" response. */
    public boolean isConfigured() {
        return notBlank(merchantId) && notBlank(apiId) && notBlank(apiKey);
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> postToGateway(String channel, Map<String, Object> data) {
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

    @SuppressWarnings("unchecked")
    public Map<String, Object> checkStatus(String referenceNo) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("api_id", apiId);
        body.put("api_key", apiKey);
        body.put("reference_no", referenceNo);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            Map<String, Object> response = restTemplate.postForObject(paymentStatusUrl, request, Map.class);
            return response != null ? response : Map.of();
        } catch (Exception e) {
            log.error("ZynlePay status check failed (reference_no={}): {}", referenceNo, e.getMessage());
            throw new RuntimeException("Unable to reach payment gateway for status check", e);
        }
    }
}
