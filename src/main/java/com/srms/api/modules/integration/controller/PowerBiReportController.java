package com.srms.api.modules.integration.controller;

import com.srms.api.common.ApiResponse;
import com.srms.api.modules.integration.dto.PowerBiReportDto;
import com.srms.api.modules.integration.entity.IntegrationConfig;
import com.srms.api.modules.integration.service.PowerBiReportService;
import com.srms.api.security.RoleGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** CRUD for a school's (or the platform's) Power BI reports — a school may publish more than
 * one dashboard, each with its own workspace/report/dataset. Super-admin only. */
@RestController
@RequestMapping("/api/schools/{schoolId}/powerbi-reports")
@RequiredArgsConstructor
public class PowerBiReportController {
    private final PowerBiReportService service;

    @GetMapping
    public ResponseEntity<ApiResponse<List<PowerBiReportDto>>> list(@PathVariable String schoolId, Authentication auth) {
        RoleGuard.requireSchoolAccountManager(auth);
        return ResponseEntity.ok(ApiResponse.ok(service.list(IntegrationConfig.ScopeType.SCHOOL, schoolId)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PowerBiReportDto>> create(
            @PathVariable String schoolId, @RequestBody PowerBiReportDto dto, Authentication auth) {
        RoleGuard.requireSchoolAccountManager(auth);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(service.create(IntegrationConfig.ScopeType.SCHOOL, schoolId, dto)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PowerBiReportDto>> update(
            @PathVariable String schoolId, @PathVariable String id, @RequestBody PowerBiReportDto dto, Authentication auth) {
        RoleGuard.requireSchoolAccountManager(auth);
        return ResponseEntity.ok(ApiResponse.ok(service.update(IntegrationConfig.ScopeType.SCHOOL, schoolId, id, dto)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String schoolId, @PathVariable String id, Authentication auth) {
        RoleGuard.requireSchoolAccountManager(auth);
        service.delete(IntegrationConfig.ScopeType.SCHOOL, schoolId, id);
        return ResponseEntity.ok(ApiResponse.ok("Report removed", null));
    }
}
