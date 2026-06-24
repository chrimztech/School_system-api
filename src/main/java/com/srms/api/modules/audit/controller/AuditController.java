package com.srms.api.modules.audit.controller;

import com.srms.api.common.ApiResponse;
import com.srms.api.modules.audit.entity.AuditEvent;
import com.srms.api.modules.audit.service.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @RequestMapping("/api/schools/{schoolId}/audit") @RequiredArgsConstructor
public class AuditController {
    private final AuditService auditService;

    @GetMapping public ResponseEntity<ApiResponse<List<AuditEvent>>> getAll(@PathVariable String schoolId) { return ResponseEntity.ok(ApiResponse.ok(auditService.findAll(schoolId))); }
    @PostMapping public ResponseEntity<ApiResponse<AuditEvent>> create(@PathVariable String schoolId, @RequestBody AuditEvent event) { return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(auditService.create(schoolId, event))); }
}
