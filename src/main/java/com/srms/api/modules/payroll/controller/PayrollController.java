package com.srms.api.modules.payroll.controller;

import com.srms.api.common.ApiResponse;
import com.srms.api.modules.payroll.entity.PayrollRun;
import com.srms.api.modules.payroll.entity.PayslipEntry;
import com.srms.api.modules.payroll.service.PayrollService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @RequestMapping("/api/schools/{schoolId}/payroll") @RequiredArgsConstructor
public class PayrollController {
    private final PayrollService payrollService;
    @GetMapping("/runs") public ResponseEntity<ApiResponse<List<PayrollRun>>> getRuns(@PathVariable String schoolId) { return ResponseEntity.ok(ApiResponse.ok(payrollService.getRuns(schoolId))); }
    @PostMapping("/runs") public ResponseEntity<ApiResponse<PayrollRun>> createRun(@PathVariable String schoolId, @RequestBody PayrollRun run) { return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(payrollService.createRun(schoolId, run))); }
    @GetMapping("/runs/{id}/payslips") public ResponseEntity<ApiResponse<List<PayslipEntry>>> getPayslips(@PathVariable String schoolId, @PathVariable String id) { return ResponseEntity.ok(ApiResponse.ok(payrollService.getPayslips(schoolId, id))); }
    @PostMapping("/runs/{id}/process") public ResponseEntity<ApiResponse<PayrollRun>> processRun(@PathVariable String schoolId, @PathVariable String id) { return ResponseEntity.ok(ApiResponse.ok(payrollService.processRun(schoolId, id))); }
}
