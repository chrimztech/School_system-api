package com.srms.api.modules.procurement.controller;

import com.srms.api.common.ApiResponse;
import com.srms.api.modules.procurement.entity.ProcurementRequest;
import com.srms.api.modules.procurement.service.ProcurementService;
import com.srms.api.security.RoleGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

// Matches the frontend's "procurement" module, which is full-access-or-nothing (no "read" tier)
// for everyone but school leadership + finance.
@RestController @RequestMapping("/api/schools/{schoolId}/procurement") @RequiredArgsConstructor
public class ProcurementController {
    private final ProcurementService procurementService;
    @GetMapping public ResponseEntity<ApiResponse<List<ProcurementRequest>>> list(@PathVariable String schoolId, Authentication auth) { RoleGuard.requireFinanceOrLeadership(auth); return ResponseEntity.ok(ApiResponse.ok(procurementService.list(schoolId))); }
    @PostMapping public ResponseEntity<ApiResponse<ProcurementRequest>> create(@PathVariable String schoolId, @RequestBody ProcurementRequest r, Authentication auth) { RoleGuard.requireFinanceOrLeadership(auth); return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(procurementService.create(schoolId, r))); }
    @PutMapping("/{id}") public ResponseEntity<ApiResponse<ProcurementRequest>> update(@PathVariable String schoolId, @PathVariable String id, @RequestBody ProcurementRequest r, Authentication auth) { RoleGuard.requireFinanceOrLeadership(auth); return ResponseEntity.ok(ApiResponse.ok(procurementService.update(schoolId, id, r))); }
    @PutMapping("/{id}/approve") public ResponseEntity<ApiResponse<Void>> approve(@PathVariable String schoolId, @PathVariable String id, Authentication auth) { RoleGuard.requireFinanceOrLeadership(auth); procurementService.approve(schoolId, id); return ResponseEntity.ok(ApiResponse.ok("Approved", null)); }
}
