package com.srms.api.modules.payment.controller;

import com.srms.api.common.ApiResponse;
import com.srms.api.modules.payment.dto.PaymentStatusView;
import com.srms.api.modules.payment.service.PaymentGatewayService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Unauthenticated endpoints — mounted under /api/public/** which is permitAll in SecurityConfig.
 * The callback is server-to-server from ZynlePay; the status lookup backs the frontend's post-payment
 * return page, which won't reliably have an authenticated session. Both are safe to expose because the
 * reference number is an unguessable UUID scoped to a single transaction.
 */
@Slf4j
@RestController
@RequestMapping("/api/public/payments/zynlepay")
@RequiredArgsConstructor
public class PaymentCallbackController {

    private final PaymentGatewayService paymentGatewayService;

    // ZynlePay's merchant dashboard has separate callback URL fields per channel
    // ("Card Deposit CallBack URL" and "Mobile Money CallBack URL") — both land here,
    // since the handler itself is channel-agnostic (looks the payment up by reference_no).
    @PostMapping("/callback")
    public ResponseEntity<Map<String, String>> cardCallback(@RequestBody Map<String, Object> payload) {
        return callback(payload);
    }

    @PostMapping("/momo-callback")
    public ResponseEntity<Map<String, String>> momoCallback(@RequestBody Map<String, Object> payload) {
        return callback(payload);
    }

    private ResponseEntity<Map<String, String>> callback(Map<String, Object> payload) {
        try {
            String referenceNo = paymentGatewayService.recordCallback(payload);
            if (referenceNo != null) {
                paymentGatewayService.verifyCallbackAsync(referenceNo);
            }
        } catch (Exception e) {
            log.error("Error processing ZynlePay callback: {}", e.getMessage(), e);
        }
        return ResponseEntity.ok(Map.of("response_code", "100"));
    }

    @GetMapping("/status")
    public ResponseEntity<ApiResponse<PaymentStatusView>> status(@RequestParam String referenceNo) {
        return ResponseEntity.ok(ApiResponse.ok(paymentGatewayService.publicStatus(referenceNo)));
    }
}
