package com.srms.api.modules.integration.controller;

import com.srms.api.common.ApiResponse;
import com.srms.api.modules.integration.dto.IntegrationConnectionView;
import com.srms.api.modules.integration.entity.IntegrationConnection;
import com.srms.api.modules.integration.service.IntegrationService;
import com.srms.api.security.RoleGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Connects third-party services with real API keys/secrets — same trust level as the platform's
// own credentials, so this is restricted the same way the frontend already treats it (super
// admin only), just actually enforced server-side now rather than only in the UI's own role
// check (which any direct API call could previously bypass entirely).
@RestController
@RequestMapping("/api/schools/{schoolId}/integrations")
@RequiredArgsConstructor
public class IntegrationController {
    private final IntegrationService integrationService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<IntegrationConnectionView>>> list(@PathVariable String schoolId, Authentication auth) {
        RoleGuard.requireSuperAdmin(auth);
        return ResponseEntity.ok(ApiResponse.ok(integrationService.list(schoolId).stream().map(IntegrationConnectionView::from).toList()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<IntegrationConnectionView>> create(@PathVariable String schoolId, @RequestBody IntegrationConnection connection, Authentication auth) {
        RoleGuard.requireSuperAdmin(auth);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(IntegrationConnectionView.from(integrationService.createOrConnect(schoolId, connection))));
    }

    @PatchMapping("/{code}")
    public ResponseEntity<ApiResponse<IntegrationConnectionView>> update(@PathVariable String schoolId, @PathVariable String code, @RequestBody IntegrationConnection patch, Authentication auth) {
        RoleGuard.requireSuperAdmin(auth);
        return ResponseEntity.ok(ApiResponse.ok(IntegrationConnectionView.from(integrationService.update(schoolId, code, patch))));
    }
}
