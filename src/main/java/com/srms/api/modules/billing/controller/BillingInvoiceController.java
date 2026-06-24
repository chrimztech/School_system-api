package com.srms.api.modules.billing.controller;

import com.srms.api.common.ApiResponse;
import com.srms.api.modules.billing.entity.BillingInvoice;
import com.srms.api.modules.billing.service.BillingInvoiceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @RequestMapping("/api/schools/{schoolId}/billing/invoices") @RequiredArgsConstructor
public class BillingInvoiceController {
    private final BillingInvoiceService service;

    @GetMapping
    public ResponseEntity<ApiResponse<List<BillingInvoice>>> getAll(@PathVariable String schoolId) {
        return ResponseEntity.ok(ApiResponse.ok(service.findAll(schoolId)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<BillingInvoice>> create(@PathVariable String schoolId, @RequestBody BillingInvoice invoice) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(service.create(schoolId, invoice)));
    }

    @PatchMapping("/{id}/pay")
    public ResponseEntity<ApiResponse<BillingInvoice>> markPaid(@PathVariable String schoolId, @PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.ok(service.markPaid(schoolId, id)));
    }
}
