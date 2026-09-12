package com.srms.api.modules.integration.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.srms.api.modules.integration.client.AfricasTalkingSmsClient;
import com.srms.api.modules.integration.client.AirtelMoneyClient;
import com.srms.api.modules.integration.client.MtnMomoClient;
import com.srms.api.modules.integration.client.ZoomClient;
import com.srms.api.modules.integration.entity.IntegrationConfig;
import com.srms.api.modules.integration.entity.IntegrationEventLog;
import com.srms.api.modules.integration.service.IntegrationConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.Map;

/**
 * Inbound webhook receivers — one per provider whose callback URL is shown on that provider's
 * configuration form (see IntegrationConfigService.PROVIDERS). Unauthenticated (mounted on paths
 * permitAll in SecurityConfig) since the provider calls these directly, with no SRMS session to
 * send; each one verifies the payload against that school's own configured secret instead.
 *
 * No school has yet wired a real collection/payment through MTN or Airtel (see those clients'
 * class-level notes — only the OAuth handshake is implemented so far), so these two receivers
 * currently just verify, log, and acknowledge; there's no FeePayment row to update yet. That
 * wiring is real follow-up work once a collection request is actually initiated through them.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class IntegrationWebhookController {
    private final IntegrationConfigService configService;
    private final ObjectMapper objectMapper;

    @PostMapping("/api/payments/callbacks/mtn-momo/{schoolId}")
    public ResponseEntity<Void> mtnMomoCallback(
            @PathVariable String schoolId, @RequestBody String rawBody,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        String expectedSecret = configService.resolveCredential(MtnMomoClient.CODE, schoolId, "callbackAuthSecret").orElse(null);
        boolean verified = expectedSecret == null || expectedSecret.isBlank() || matchesBearer(authHeader, expectedSecret);
        logWebhook(MtnMomoClient.CODE, schoolId, verified, rawBody);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/api/payments/callbacks/airtel-money/{schoolId}")
    public ResponseEntity<Void> airtelMoneyCallback(
            @PathVariable String schoolId, @RequestBody String rawBody,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        String expectedSecret = configService.resolveCredential(AirtelMoneyClient.CODE, schoolId, "webhookSecret").orElse(null);
        boolean verified = expectedSecret == null || expectedSecret.isBlank() || matchesBearer(authHeader, expectedSecret);
        logWebhook(AirtelMoneyClient.CODE, schoolId, verified, rawBody);
        return ResponseEntity.ok().build();
    }

    /** Africa's Talking delivery reports/incoming messages are posted as form-encoded fields, not
     * JSON, and the platform doesn't sign them — there's nothing to verify against, so this just
     * records what arrived. */
    @PostMapping("/api/integrations/callbacks/africas-talking/{schoolId}")
    public ResponseEntity<Void> africasTalkingCallback(@PathVariable String schoolId, @RequestBody(required = false) String rawBody) {
        logWebhook(AfricasTalkingSmsClient.CODE, schoolId, true, rawBody);
        return ResponseEntity.ok().build();
    }

    /**
     * Zoom webhooks. Two real behaviors required by Zoom's own protocol
     * (https://developers.zoom.us/docs/api/webhooks/):
     *  1. "endpoint.url_validation" — sent once when the webhook subscription is first activated;
     *     must be answered with the same plainToken plus an HMAC-SHA256 of it, or Zoom refuses to
     *     activate the subscription.
     *  2. Every other event carries an "x-zm-signature" header ("v0=" + HMAC-SHA256 hex of
     *     "v0:{timestamp}:{rawBody}") that must be checked against the configured webhook secret
     *     token before trusting the payload.
     */
    @PostMapping("/api/integrations/callbacks/zoom/{schoolId}")
    public ResponseEntity<Map<String, String>> zoomCallback(
            @PathVariable String schoolId, @RequestBody String rawBody,
            @RequestHeader(value = "x-zm-signature", required = false) String signature,
            @RequestHeader(value = "x-zm-request-timestamp", required = false) String timestamp) {
        String secret = configService.resolveCredential(ZoomClient.CODE, schoolId, "webhookSecretToken").orElse(null);

        JsonNode node = parseQuietly(rawBody);
        String event = node != null && node.has("event") ? node.get("event").asText() : null;

        if ("endpoint.url_validation".equals(event) && node.has("payload") && node.get("payload").has("plainToken")) {
            String plainToken = node.get("payload").get("plainToken").asText();
            if (secret == null || secret.isBlank()) {
                log.warn("Zoom URL validation received for school {} but no webhook secret token is configured", schoolId);
                return ResponseEntity.ok(Map.of("plainToken", plainToken, "encryptedToken", plainToken));
            }
            String encrypted = hmacSha256Hex(secret, plainToken);
            logWebhook(ZoomClient.CODE, schoolId, true, rawBody);
            return ResponseEntity.ok(Map.of("plainToken", plainToken, "encryptedToken", encrypted));
        }

        boolean verified = secret == null || secret.isBlank() || verifyZoomSignature(secret, signature, timestamp, rawBody);
        logWebhook(ZoomClient.CODE, schoolId, verified, rawBody);
        return ResponseEntity.ok(Map.of());
    }

    private void logWebhook(String providerCode, String schoolId, boolean verified, String rawBody) {
        String message = verified ? "Webhook received" : "Webhook received but failed signature verification — check the configured secret";
        String metadata = rawBody != null && rawBody.length() > 2000 ? rawBody.substring(0, 2000) : rawBody;
        configService.recordEvent(IntegrationConfig.ScopeType.SCHOOL, schoolId, providerCode,
                IntegrationEventLog.EventType.WEBHOOK, verified, message, metadata, null);
        if (!verified) {
            log.warn("{} webhook for school {} failed signature verification", providerCode, schoolId);
        }
    }

    private boolean matchesBearer(String authHeader, String expectedSecret) {
        if (authHeader == null) return false;
        String token = authHeader.startsWith("Bearer ") ? authHeader.substring(7) : authHeader;
        return token.equals(expectedSecret);
    }

    private boolean verifyZoomSignature(String secret, String signature, String timestamp, String rawBody) {
        if (signature == null || timestamp == null) return false;
        String expected = "v0=" + hmacSha256Hex(secret, "v0:" + timestamp + ":" + rawBody);
        return expected.equals(signature);
    }

    private String hmacSha256Hex(String secret, String message) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            log.error("HMAC computation failed: {}", e.getMessage());
            return "";
        }
    }

    private JsonNode parseQuietly(String rawBody) {
        try {
            return objectMapper.readTree(rawBody);
        } catch (Exception e) {
            return null;
        }
    }
}
