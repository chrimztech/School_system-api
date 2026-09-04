package com.srms.api.modules.admission.controller;

import com.srms.api.common.ApiResponse;
import com.srms.api.exception.ForbiddenException;
import com.srms.api.modules.admission.entity.AdmissionApplication;
import com.srms.api.modules.admission.service.AdmissionService;
import com.srms.api.security.ModuleAccessService;
import com.srms.api.security.RoleGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Set;

// Matches the frontend's "admissions" module: full access (read+write) for school leadership,
// read-only for finance, no access at all for every other role.
@RestController @RequestMapping("/api/schools/{schoolId}/admissions") @RequiredArgsConstructor
public class AdmissionController {
    private final AdmissionService admissionService;
    private final ModuleAccessService moduleAccessService;

    // Mirrors RoleGuard's own (private) SCHOOL_ACCOUNT_MANAGERS / FINANCE_AND_LEADERSHIP sets —
    // reconstructed here so the original hardcoded default can be passed through as the
    // moduleAccessService.isAllowed() defaultAllowed argument without modifying RoleGuard itself.
    private static final Set<String> SCHOOL_ACCOUNT_MANAGERS = Set.of(
            "SUPER_ADMIN", "SCHOOL_ADMIN", "PRINCIPAL", "DEPUTY_HEAD");
    private static final Set<String> FINANCE_AND_LEADERSHIP = Set.of(
            "SUPER_ADMIN", "SCHOOL_ADMIN", "PRINCIPAL", "DEPUTY_HEAD", "FINANCE");

    @GetMapping public ResponseEntity<ApiResponse<List<AdmissionApplication>>> getAll(@PathVariable String schoolId, Authentication auth) { requireRead(schoolId, auth); return ResponseEntity.ok(ApiResponse.ok(admissionService.getAll(schoolId))); }
    @PostMapping public ResponseEntity<ApiResponse<AdmissionApplication>> create(@PathVariable String schoolId, @RequestBody AdmissionApplication app, Authentication auth) { requireManage(schoolId, auth); return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(admissionService.create(schoolId, app))); }
    @GetMapping("/{id}") public ResponseEntity<ApiResponse<AdmissionApplication>> getOne(@PathVariable String schoolId, @PathVariable String id, Authentication auth) { requireRead(schoolId, auth); return ResponseEntity.ok(ApiResponse.ok(admissionService.get(schoolId, id))); }
    @PutMapping("/{id}") public ResponseEntity<ApiResponse<AdmissionApplication>> update(@PathVariable String schoolId, @PathVariable String id, @RequestBody AdmissionApplication app, Authentication auth) { requireManage(schoolId, auth); return ResponseEntity.ok(ApiResponse.ok(admissionService.update(schoolId, id, app))); }
    @PutMapping("/{id}/accept") public ResponseEntity<ApiResponse<AdmissionApplication>> accept(@PathVariable String schoolId, @PathVariable String id, Authentication auth) { requireManage(schoolId, auth); return ResponseEntity.ok(ApiResponse.ok(admissionService.accept(schoolId, id))); }
    @PutMapping("/{id}/reject") public ResponseEntity<ApiResponse<AdmissionApplication>> reject(@PathVariable String schoolId, @PathVariable String id, Authentication auth) { requireManage(schoolId, auth); return ResponseEntity.ok(ApiResponse.ok(admissionService.reject(schoolId, id))); }

    private void requireRead(String schoolId, Authentication auth) {
        if (!moduleAccessService.isAllowed(schoolId, auth, "admissions", "read", FINANCE_AND_LEADERSHIP.contains(RoleGuard.roleOf(auth)))) {
            throw new ForbiddenException("Your role cannot access this module");
        }
    }

    private void requireManage(String schoolId, Authentication auth) {
        if (!moduleAccessService.isAllowed(schoolId, auth, "admissions", "full", SCHOOL_ACCOUNT_MANAGERS.contains(RoleGuard.roleOf(auth)))) {
            throw new ForbiddenException("Your role cannot manage school accounts or permissions");
        }
    }
}
