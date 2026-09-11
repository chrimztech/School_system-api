package com.srms.api.modules.billing.controller;

import com.srms.api.common.ApiResponse;
import com.srms.api.modules.billing.entity.BillingInvoice;
import com.srms.api.modules.billing.service.BillingInvoiceService;
import com.srms.api.security.RoleGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/** Mirrors the frontend's route-access.ts policy for /billing: {@code allowedRoles:
 * ["super_admin"]} — a school's subscription invoices to the platform are a platform-admin
 * concern, not even SCHOOL_ADMIN's. That policy only ever stopped navigation; it never stopped
 * a direct API call. */
@RestController @RequestMapping("/api/schools/{schoolId}/billing/invoices") @RequiredArgsConstructor
public class BillingInvoiceController {
    private final BillingInvoiceService service;

    @GetMapping
    public ResponseEntity<ApiResponse<List<BillingInvoice>>> getAll(@PathVariable String schoolId, Authentication auth) {
        RoleGuard.requireSuperAdmin(auth);
        return ResponseEntity.ok(ApiResponse.ok(service.findAll(schoolId)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<BillingInvoice>> create(@PathVariable String schoolId, @RequestBody BillingInvoice invoice, Authentication auth) {
        RoleGuard.requireSuperAdmin(auth);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(service.create(schoolId, invoice)));
    }

    @PatchMapping("/{id}/pay")
    public ResponseEntity<ApiResponse<BillingInvoice>> markPaid(@PathVariable String schoolId, @PathVariable String id, Authentication auth) {
        RoleGuard.requireSuperAdmin(auth);
        return ResponseEntity.ok(ApiResponse.ok(service.markPaid(schoolId, id)));
    }
}
