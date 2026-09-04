package com.srms.api.modules.procurement.controller;

import com.srms.api.common.ApiResponse;
import com.srms.api.exception.ForbiddenException;
import com.srms.api.modules.procurement.entity.ProcurementRequest;
import com.srms.api.modules.procurement.service.ProcurementService;
import com.srms.api.security.ModuleAccessService;
import com.srms.api.security.RoleGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Set;

// Matches the frontend's "procurement" module, which is full-access-or-nothing (no "read" tier)
// for everyone but school leadership + finance.
@RestController @RequestMapping("/api/schools/{schoolId}/procurement") @RequiredArgsConstructor
public class ProcurementController {
    private final ProcurementService procurementService;
    private final ModuleAccessService moduleAccessService;

    // Mirrors RoleGuard's own (private) FINANCE_AND_LEADERSHIP set.
    private static final Set<String> FINANCE_AND_LEADERSHIP = Set.of(
            "SUPER_ADMIN", "SCHOOL_ADMIN", "PRINCIPAL", "DEPUTY_HEAD", "FINANCE");

    @GetMapping public ResponseEntity<ApiResponse<List<ProcurementRequest>>> list(@PathVariable String schoolId, Authentication auth) { requireRead(schoolId, auth); return ResponseEntity.ok(ApiResponse.ok(procurementService.list(schoolId))); }
    @PostMapping public ResponseEntity<ApiResponse<ProcurementRequest>> create(@PathVariable String schoolId, @RequestBody ProcurementRequest r, Authentication auth) { requireManage(schoolId, auth); return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(procurementService.create(schoolId, r))); }
    @PutMapping("/{id}") public ResponseEntity<ApiResponse<ProcurementRequest>> update(@PathVariable String schoolId, @PathVariable String id, @RequestBody ProcurementRequest r, Authentication auth) { requireManage(schoolId, auth); return ResponseEntity.ok(ApiResponse.ok(procurementService.update(schoolId, id, r))); }
    @PutMapping("/{id}/approve") public ResponseEntity<ApiResponse<Void>> approve(@PathVariable String schoolId, @PathVariable String id, Authentication auth) { requireManage(schoolId, auth); procurementService.approve(schoolId, id); return ResponseEntity.ok(ApiResponse.ok("Approved", null)); }

    private void requireRead(String schoolId, Authentication auth) {
        if (!moduleAccessService.isAllowed(schoolId, auth, "procurement", "read", FINANCE_AND_LEADERSHIP.contains(RoleGuard.roleOf(auth)))) {
            throw new ForbiddenException("Your role cannot access this module");
        }
    }

    private void requireManage(String schoolId, Authentication auth) {
        if (!moduleAccessService.isAllowed(schoolId, auth, "procurement", "full", FINANCE_AND_LEADERSHIP.contains(RoleGuard.roleOf(auth)))) {
            throw new ForbiddenException("Your role cannot access this module");
        }
    }
}
