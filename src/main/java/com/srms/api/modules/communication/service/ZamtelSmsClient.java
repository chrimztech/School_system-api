package com.srms.api.modules.communication.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * Thin client for the Zamtel BulkSMS "KSMS" third-party interface (v2.1) —
 * https://bulksms.zamtel.co.zm/api. Unlike a typical JSON REST API, send() is a plain GET with
 * every field (including the message) embedded as URL path segments, so building that URL by
 * hand (rather than via a request body) is the whole client.
 */
@Slf4j
@Service
public class ZamtelSmsClient {

    @Value("${zamtel.bulksms.base-url:https://bulksms.zamtel.co.zm/api}")
    private String baseUrl;

    @Value("${zamtel.bulksms.api-key:}")
    private String apiKey;

    @Value("${zamtel.bulksms.sender-id:DCL}")
    private String defaultSenderId;

    private final RestTemplate restTemplate = new RestTemplate();

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    /**
     * Queues one message to a batch of already-normalized numbers (digits only, e.g.
     * "260977000000" — see PhoneUtils.normalize). Returns true if the gateway accepted the
     * batch (HTTP 202); false on any error, with the reason logged rather than thrown, since a
     * dispatch failure for one channel/batch shouldn't block the others.
     *
     * Batch size is intentionally small (see NotificationService.SMS_BATCH_SIZE): every contact
     * is a literal segment in the request URL, not a request body, so there's a real length
     * ceiling most servers/proxies enforce — unlike a JSON-bodied API where batch size is only
     * about the provider's own per-request recipient cap.
     *
     * @param senderId the sending school's own approved sender ID (School.smsSenderId), or
     *                 null/blank to fall back to zamtel.bulksms.sender-id. Zamtel requires each
     *                 sender ID to be individually pre-registered and approved on the account —
     *                 an unapproved id fails the whole request with "Invalid api_key and/or
     *                 sender id", not a per-recipient error, so this can't be the school's name
     *                 unless that exact string has actually been approved for them.
     */
    public boolean send(List<String> contacts, String message, String senderId) {
        if (contacts.isEmpty()) return true;
        if (!isConfigured()) {
            log.warn("Zamtel BulkSMS not configured (zamtel.bulksms.api-key missing) — skipping {} recipients", contacts.size());
            return false;
        }

        String effectiveSenderId = senderId != null && !senderId.isBlank() ? senderId : defaultSenderId;
        String contactsSegment = "[" + String.join(",", contacts) + "]";
        String encodedMessage = UriUtils.encodePathSegment(message, StandardCharsets.UTF_8);
        String url = baseUrl + "/v2.1/action/send/api_key/" + apiKey
                + "/contacts/" + contactsSegment
                + "/senderId/" + effectiveSenderId
                + "/message/" + encodedMessage;

        try {
            restTemplate.getForObject(url, String.class);
            return true;
        } catch (HttpClientErrorException e) {
            log.error("Zamtel BulkSMS send failed for {} recipients (senderId={}): {} — {}", contacts.size(), effectiveSenderId, e.getStatusCode(), e.getResponseBodyAsString());
            return false;
        } catch (Exception e) {
            log.error("Zamtel BulkSMS send failed for {} recipients (senderId={}): {}", contacts.size(), effectiveSenderId, e.getMessage());
            return false;
        }
    }

    /** Remaining SMS credit, or -1 if the gateway is unreachable or the key isn't configured. */
    @SuppressWarnings("unchecked")
    public long getBalance() {
        if (!isConfigured()) return -1;
        try {
            Map<String, Object> response = restTemplate.getForObject(
                    baseUrl + "/sms/balance?key=" + apiKey, Map.class);
            Object inner = response != null ? response.get("responseObject") : null;
            Object balance = inner instanceof Map ? ((Map<String, Object>) inner).get("sms_balance") : null;
            return balance instanceof Number ? ((Number) balance).longValue() : -1;
        } catch (Exception e) {
            log.error("Zamtel BulkSMS balance check failed: {}", e.getMessage());
            return -1;
        }
    }
}
