package com.srms.api.modules.payroll.controller;

import com.srms.api.common.ApiResponse;
import com.srms.api.exception.ForbiddenException;
import com.srms.api.modules.payroll.dto.PayrollStaffView;
import com.srms.api.modules.payroll.entity.PayrollRun;
import com.srms.api.modules.payroll.entity.PayslipEntry;
import com.srms.api.modules.payroll.service.PayrollService;
import com.srms.api.security.ModuleAccessService;
import com.srms.api.security.RoleGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Set;

// Salary data and the ability to trigger a payroll run — restricted to the same roles the
// frontend's own "accounting" module marks as full access (school leadership + finance); every
// other role is "false" there, not "read", so even listing runs is off-limits, not just writes.
@RestController @RequestMapping("/api/schools/{schoolId}/payroll") @RequiredArgsConstructor
public class PayrollController {
    private final PayrollService payrollService;
    private final ModuleAccessService moduleAccessService;

    // Mirrors RoleGuard's own (private) FINANCE_AND_LEADERSHIP set.
    private static final Set<String> FINANCE_AND_LEADERSHIP = Set.of(
            "SUPER_ADMIN", "SCHOOL_ADMIN", "PRINCIPAL", "DEPUTY_HEAD", "FINANCE");

    @GetMapping("/staff") public ResponseEntity<ApiResponse<List<PayrollStaffView>>> getStaff(@PathVariable String schoolId, Authentication auth) { requireRead(schoolId, auth); return ResponseEntity.ok(ApiResponse.ok(payrollService.getUnifiedStaff(schoolId))); }
    @GetMapping("/runs") public ResponseEntity<ApiResponse<List<PayrollRun>>> getRuns(@PathVariable String schoolId, Authentication auth) { requireRead(schoolId, auth); return ResponseEntity.ok(ApiResponse.ok(payrollService.getRuns(schoolId))); }
    @PostMapping("/runs") public ResponseEntity<ApiResponse<PayrollRun>> createRun(@PathVariable String schoolId, @RequestBody PayrollRun run, Authentication auth) { requireManage(schoolId, auth); return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(payrollService.createRun(schoolId, run))); }
    @GetMapping("/runs/{id}/payslips") public ResponseEntity<ApiResponse<List<PayslipEntry>>> getPayslips(@PathVariable String schoolId, @PathVariable String id, Authentication auth) { requireRead(schoolId, auth); return ResponseEntity.ok(ApiResponse.ok(payrollService.getPayslips(schoolId, id))); }
    @PostMapping("/runs/{id}/process") public ResponseEntity<ApiResponse<PayrollRun>> processRun(@PathVariable String schoolId, @PathVariable String id, Authentication auth) { requireManage(schoolId, auth); return ResponseEntity.ok(ApiResponse.ok(payrollService.processRun(schoolId, id))); }

    private void requireRead(String schoolId, Authentication auth) {
        if (!moduleAccessService.isAllowed(schoolId, auth, "accounting", "read", FINANCE_AND_LEADERSHIP.contains(RoleGuard.roleOf(auth)))) {
            throw new ForbiddenException("Your role cannot access this module");
        }
    }

    private void requireManage(String schoolId, Authentication auth) {
        if (!moduleAccessService.isAllowed(schoolId, auth, "accounting", "full", FINANCE_AND_LEADERSHIP.contains(RoleGuard.roleOf(auth)))) {
            throw new ForbiddenException("Your role cannot access this module");
        }
    }
}
