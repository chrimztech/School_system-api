package com.srms.api.modules.audit.controller;

import com.srms.api.common.ApiResponse;
import com.srms.api.common.PageRequestUtil;
import com.srms.api.common.PageResponse;
import com.srms.api.modules.audit.entity.AuditEvent;
import com.srms.api.modules.audit.service.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/schools/{schoolId}/audit") @RequiredArgsConstructor
public class AuditController {
    private final AuditService auditService;

    @GetMapping
    public ResponseEntity<ApiResponse<?>> getAll(
            @PathVariable String schoolId,
            @RequestParam(required = false) Integer page, @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sortBy, @RequestParam(required = false) String sortDir) {
        Pageable pageable = PageRequestUtil.build(page, size, sortBy, sortDir);
        if (pageable == null) return ResponseEntity.ok(ApiResponse.ok(auditService.findAll(schoolId)));
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.of(auditService.findAllPaged(schoolId, pageable))));
    }
    @PostMapping public ResponseEntity<ApiResponse<AuditEvent>> create(@PathVariable String schoolId, @RequestBody AuditEvent event) { return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(auditService.create(schoolId, event))); }
}
