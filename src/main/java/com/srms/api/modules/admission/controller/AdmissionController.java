package com.srms.api.modules.admission.controller;

import com.srms.api.common.ApiResponse;
import com.srms.api.modules.admission.entity.AdmissionApplication;
import com.srms.api.modules.admission.service.AdmissionService;
import com.srms.api.security.RoleGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

// Matches the frontend's "admissions" module: full access (read+write) for school leadership,
// read-only for finance, no access at all for every other role.
@RestController @RequestMapping("/api/schools/{schoolId}/admissions") @RequiredArgsConstructor
public class AdmissionController {
    private final AdmissionService admissionService;
    @GetMapping public ResponseEntity<ApiResponse<List<AdmissionApplication>>> getAll(@PathVariable String schoolId, Authentication auth) { RoleGuard.requireFinanceOrLeadership(auth); return ResponseEntity.ok(ApiResponse.ok(admissionService.getAll(schoolId))); }
    @PostMapping public ResponseEntity<ApiResponse<AdmissionApplication>> create(@PathVariable String schoolId, @RequestBody AdmissionApplication app, Authentication auth) { RoleGuard.requireSchoolAccountManager(auth); return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(admissionService.create(schoolId, app))); }
    @GetMapping("/{id}") public ResponseEntity<ApiResponse<AdmissionApplication>> getOne(@PathVariable String schoolId, @PathVariable String id, Authentication auth) { RoleGuard.requireFinanceOrLeadership(auth); return ResponseEntity.ok(ApiResponse.ok(admissionService.get(schoolId, id))); }
    @PutMapping("/{id}") public ResponseEntity<ApiResponse<AdmissionApplication>> update(@PathVariable String schoolId, @PathVariable String id, @RequestBody AdmissionApplication app, Authentication auth) { RoleGuard.requireSchoolAccountManager(auth); return ResponseEntity.ok(ApiResponse.ok(admissionService.update(schoolId, id, app))); }
    @PutMapping("/{id}/accept") public ResponseEntity<ApiResponse<AdmissionApplication>> accept(@PathVariable String schoolId, @PathVariable String id, Authentication auth) { RoleGuard.requireSchoolAccountManager(auth); return ResponseEntity.ok(ApiResponse.ok(admissionService.accept(schoolId, id))); }
    @PutMapping("/{id}/reject") public ResponseEntity<ApiResponse<AdmissionApplication>> reject(@PathVariable String schoolId, @PathVariable String id, Authentication auth) { RoleGuard.requireSchoolAccountManager(auth); return ResponseEntity.ok(ApiResponse.ok(admissionService.reject(schoolId, id))); }
}
