package com.srms.api.modules.risk.controller;

import com.srms.api.common.ApiResponse;
import com.srms.api.modules.risk.entity.RiskEntry;
import com.srms.api.modules.risk.service.RiskService;
import com.srms.api.security.RoleGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

// Reads stay open — the frontend's "risk-register" module gives teacher "read" access — but
// writes are school leadership + finance only, the only roles the matrix marks as full access.
@RestController @RequestMapping("/api/schools/{schoolId}/risk-register") @RequiredArgsConstructor
public class RiskController {
    private final RiskService riskService;
    @GetMapping public ResponseEntity<ApiResponse<List<RiskEntry>>> list(@PathVariable String schoolId) { return ResponseEntity.ok(ApiResponse.ok(riskService.list(schoolId))); }
    @PostMapping public ResponseEntity<ApiResponse<RiskEntry>> create(@PathVariable String schoolId, @RequestBody RiskEntry r, Authentication auth) { RoleGuard.requireFinanceOrLeadership(auth); return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(riskService.create(schoolId, r))); }
    @PutMapping("/{id}") public ResponseEntity<ApiResponse<RiskEntry>> update(@PathVariable String schoolId, @PathVariable String id, @RequestBody RiskEntry r, Authentication auth) { RoleGuard.requireFinanceOrLeadership(auth); return ResponseEntity.ok(ApiResponse.ok(riskService.update(schoolId, id, r))); }
}
