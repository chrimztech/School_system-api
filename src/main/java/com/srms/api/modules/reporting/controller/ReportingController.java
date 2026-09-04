package com.srms.api.modules.reporting.controller;

import com.srms.api.common.ApiResponse;
import com.srms.api.modules.reporting.entity.SavedReport;
import com.srms.api.modules.reporting.service.ReportingService;
import com.srms.api.security.RoleGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Reads stay open — the frontend's "reporting" module gives teacher/hod "read" access — but
// writes are school leadership + finance only, the only roles the matrix marks as full access.
@RestController
@RequestMapping("/api/schools/{schoolId}/reporting")
@RequiredArgsConstructor
public class ReportingController {
    private final ReportingService reportingService;

    @GetMapping("/reports")
    public ResponseEntity<ApiResponse<List<SavedReport>>> list(@PathVariable String schoolId) {
        return ResponseEntity.ok(ApiResponse.ok(reportingService.list(schoolId)));
    }

    @PostMapping("/reports")
    public ResponseEntity<ApiResponse<SavedReport>> create(@PathVariable String schoolId, @RequestBody SavedReport report, Authentication auth) {
        RoleGuard.requireFinanceOrLeadership(auth);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(reportingService.create(schoolId, report)));
    }

    @PutMapping("/reports/{id}")
    public ResponseEntity<ApiResponse<SavedReport>> update(@PathVariable String schoolId, @PathVariable String id, @RequestBody SavedReport report, Authentication auth) {
        RoleGuard.requireFinanceOrLeadership(auth);
        return ResponseEntity.ok(ApiResponse.ok(reportingService.update(schoolId, id, report)));
    }

    @DeleteMapping("/reports/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String schoolId, @PathVariable String id, Authentication auth) {
        RoleGuard.requireFinanceOrLeadership(auth);
        reportingService.delete(schoolId, id);
        return ResponseEntity.ok(ApiResponse.ok("Report deleted", null));
    }
}
