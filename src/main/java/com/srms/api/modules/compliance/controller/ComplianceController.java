package com.srms.api.modules.compliance.controller;

import com.srms.api.common.ApiResponse;
import com.srms.api.modules.compliance.entity.ComplianceItem;
import com.srms.api.modules.compliance.service.ComplianceService;
import com.srms.api.security.RoleGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

// The frontend's "compliance" module gives teacher/hod/finance only "read", never full — writes
// are school-leadership only, matching auth.tsx's MODULE_MATRIX exactly.
@RestController @RequestMapping("/api/schools/{schoolId}/compliance") @RequiredArgsConstructor
public class ComplianceController {
    private final ComplianceService complianceService;
    @GetMapping public ResponseEntity<ApiResponse<List<ComplianceItem>>> list(@PathVariable String schoolId) { return ResponseEntity.ok(ApiResponse.ok(complianceService.list(schoolId))); }
    @PostMapping public ResponseEntity<ApiResponse<ComplianceItem>> create(@PathVariable String schoolId, @RequestBody ComplianceItem c, Authentication auth) { RoleGuard.requireSchoolAccountManager(auth); return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(complianceService.create(schoolId, c))); }
    @PutMapping("/{id}") public ResponseEntity<ApiResponse<ComplianceItem>> update(@PathVariable String schoolId, @PathVariable String id, @RequestBody ComplianceItem c, Authentication auth) { RoleGuard.requireSchoolAccountManager(auth); return ResponseEntity.ok(ApiResponse.ok(complianceService.update(schoolId, id, c))); }
}
