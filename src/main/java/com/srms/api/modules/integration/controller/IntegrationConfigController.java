package com.srms.api.modules.integration.controller;

import com.srms.api.common.ApiResponse;
import com.srms.api.modules.integration.dto.IntegrationConfigSaveRequest;
import com.srms.api.modules.integration.dto.IntegrationConfigView;
import com.srms.api.modules.integration.entity.IntegrationConfig;
import com.srms.api.modules.integration.service.IntegrationConfigService;
import com.srms.api.security.RoleGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Configuration CRUD for every third-party integration, both scopes. Every endpoint is
 * super-admin only — configuring a payment/SMS/SSO/analytics credential is at least as sensitive
 * as the platform workspace itself. Live actions (test connection, create meeting, publish
 * snapshot, ECZ sync) stay on IntegrationActionsController, which now reads through this same
 * storage.
 */
@RestController
@RequiredArgsConstructor
public class IntegrationConfigController {
    private final IntegrationConfigService service;

    @GetMapping("/api/platform/integration-configs")
    public ResponseEntity<ApiResponse<List<IntegrationConfigView>>> listPlatform(Authentication auth) {
        RoleGuard.requireSuperAdmin(auth);
        return ResponseEntity.ok(ApiResponse.ok(service.list(IntegrationConfig.ScopeType.PLATFORM, IntegrationConfig.PLATFORM_SCOPE_SCHOOL_ID)));
    }

    @PutMapping("/api/platform/integration-configs/{providerCode}")
    public ResponseEntity<ApiResponse<IntegrationConfigView>> savePlatform(
            @PathVariable String providerCode, @RequestBody IntegrationConfigSaveRequest request, Authentication auth) {
        RoleGuard.requireSuperAdmin(auth);
        return ResponseEntity.ok(ApiResponse.ok(service.save(
                IntegrationConfig.ScopeType.PLATFORM, IntegrationConfig.PLATFORM_SCOPE_SCHOOL_ID, providerCode, request, auth.getName())));
    }

    @GetMapping("/api/schools/{schoolId}/integration-configs")
    public ResponseEntity<ApiResponse<List<IntegrationConfigView>>> listForSchool(@PathVariable String schoolId, Authentication auth) {
        RoleGuard.requireSuperAdmin(auth);
        return ResponseEntity.ok(ApiResponse.ok(service.list(IntegrationConfig.ScopeType.SCHOOL, schoolId)));
    }

    @PutMapping("/api/schools/{schoolId}/integration-configs/{providerCode}")
    public ResponseEntity<ApiResponse<IntegrationConfigView>> saveForSchool(
            @PathVariable String schoolId, @PathVariable String providerCode,
            @RequestBody IntegrationConfigSaveRequest request, Authentication auth) {
        RoleGuard.requireSuperAdmin(auth);
        return ResponseEntity.ok(ApiResponse.ok(service.save(IntegrationConfig.ScopeType.SCHOOL, schoolId, providerCode, request, auth.getName())));
    }
}
