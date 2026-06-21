package com.srms.api.modules.integration.controller;

import com.srms.api.common.ApiResponse;
import com.srms.api.modules.integration.entity.IntegrationConnection;
import com.srms.api.modules.integration.service.IntegrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/schools/{schoolId}/integrations")
@RequiredArgsConstructor
public class IntegrationController {
    private final IntegrationService integrationService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<IntegrationConnection>>> list(@PathVariable String schoolId) {
        return ResponseEntity.ok(ApiResponse.ok(integrationService.list(schoolId)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<IntegrationConnection>> create(@PathVariable String schoolId, @RequestBody IntegrationConnection connection) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(integrationService.createOrConnect(schoolId, connection)));
    }

    @PatchMapping("/{code}")
    public ResponseEntity<ApiResponse<IntegrationConnection>> update(@PathVariable String schoolId, @PathVariable String code, @RequestBody IntegrationConnection patch) {
        return ResponseEntity.ok(ApiResponse.ok(integrationService.update(schoolId, code, patch)));
    }
}
