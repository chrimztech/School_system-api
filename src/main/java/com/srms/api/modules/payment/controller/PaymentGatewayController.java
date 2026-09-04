package com.srms.api.modules.payment.controller;

import com.srms.api.common.ApiResponse;
import com.srms.api.modules.fee.entity.FeePayment;
import com.srms.api.modules.payment.dto.CardPaymentRequest;
import com.srms.api.modules.payment.dto.MomoPaymentRequest;
import com.srms.api.modules.payment.service.PaymentGatewayService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/schools/{schoolId}/fees/payments")
@RequiredArgsConstructor
public class PaymentGatewayController {

    private final PaymentGatewayService paymentGatewayService;

    @GetMapping("/gateway-status")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> gatewayStatus() {
        return ResponseEntity.ok(ApiResponse.ok(Map.of("available", paymentGatewayService.isGatewayAvailable())));
    }

    @PostMapping("/card/initiate")
    public ResponseEntity<ApiResponse<Map<String, Object>>> initiateCard(@PathVariable String schoolId, @RequestBody CardPaymentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(paymentGatewayService.initiateCardPayment(schoolId, request)));
    }

    @PostMapping("/momo/initiate")
    public ResponseEntity<ApiResponse<Map<String, Object>>> initiateMomo(@PathVariable String schoolId, @RequestBody MomoPaymentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(paymentGatewayService.initiateMomoPayment(schoolId, request)));
    }

    @GetMapping("/{paymentId}/status")
    public ResponseEntity<ApiResponse<FeePayment>> status(@PathVariable String schoolId, @PathVariable String paymentId) {
        return ResponseEntity.ok(ApiResponse.ok(paymentGatewayService.checkPaymentStatus(schoolId, paymentId)));
    }
}
