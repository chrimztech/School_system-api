package com.srms.api.modules.payment.controller;

import com.srms.api.common.ApiResponse;
import com.srms.api.modules.payment.dto.MerchantBalanceView;
import com.srms.api.modules.payment.service.PaymentGatewayService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/platform/payments/zynlepay")
@RequiredArgsConstructor
public class PlatformPaymentController {

    private final PaymentGatewayService paymentGatewayService;

    @GetMapping("/balance")
    public ResponseEntity<ApiResponse<MerchantBalanceView>> balance(Authentication auth) {
        boolean isSuper = auth.getAuthorities().stream().anyMatch(a -> "ROLE_SUPER_ADMIN".equals(a.getAuthority()));
        if (!isSuper) {
            return ResponseEntity.status(403).body(ApiResponse.error("Only system administrators can view the payment gateway balance"));
        }
        return ResponseEntity.ok(ApiResponse.ok(paymentGatewayService.getMerchantBalance()));
    }
}
