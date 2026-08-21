package com.srms.api.modules.reporting.controller;

import com.srms.api.common.ApiResponse;
import com.srms.api.modules.reporting.entity.SavedReport;
import com.srms.api.modules.reporting.service.ReportingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    public ResponseEntity<ApiResponse<SavedReport>> create(@PathVariable String schoolId, @RequestBody SavedReport report) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(reportingService.create(schoolId, report)));
    }

    @PutMapping("/reports/{id}")
    public ResponseEntity<ApiResponse<SavedReport>> update(@PathVariable String schoolId, @PathVariable String id, @RequestBody SavedReport report) {
        return ResponseEntity.ok(ApiResponse.ok(reportingService.update(schoolId, id, report)));
    }

    @DeleteMapping("/reports/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String schoolId, @PathVariable String id) {
        reportingService.delete(schoolId, id);
        return ResponseEntity.ok(ApiResponse.ok("Report deleted", null));
    }
}
