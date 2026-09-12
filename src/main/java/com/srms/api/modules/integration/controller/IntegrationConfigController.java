package com.srms.api.modules.integration.controller;

import com.srms.api.common.ApiResponse;
import com.srms.api.exception.BusinessException;
import com.srms.api.modules.integration.dto.IntegrationConfigSaveRequest;
import com.srms.api.modules.integration.dto.IntegrationConfigView;
import com.srms.api.modules.integration.dto.IntegrationEventView;
import com.srms.api.modules.integration.entity.IntegrationConfig;
import com.srms.api.modules.integration.service.IntegrationConfigService;
import com.srms.api.security.RoleGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Base64;
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

    // School-scoped endpoints are managed by that school's own admin tier (SCHOOL_ADMIN,
    // PRINCIPAL, DEPUTY_HEAD, plus SUPER_ADMIN) — a school configuring its own MTN/Airtel/Zoom/
    // etc. account is exactly what per-school scope exists for. TenantAccessFilter already keeps
    // a non-super-admin caller confined to their own schoolId path segment. Only the platform
    // scope below (the platform's own fallback account) is super-admin only.

    @GetMapping("/api/schools/{schoolId}/integration-configs")
    public ResponseEntity<ApiResponse<List<IntegrationConfigView>>> listForSchool(@PathVariable String schoolId, Authentication auth) {
        RoleGuard.requireSchoolAccountManager(auth);
        return ResponseEntity.ok(ApiResponse.ok(service.list(IntegrationConfig.ScopeType.SCHOOL, schoolId)));
    }

    @PutMapping("/api/schools/{schoolId}/integration-configs/{providerCode}")
    public ResponseEntity<ApiResponse<IntegrationConfigView>> saveForSchool(
            @PathVariable String schoolId, @PathVariable String providerCode,
            @RequestBody IntegrationConfigSaveRequest request, Authentication auth) {
        RoleGuard.requireSchoolAccountManager(auth);
        return ResponseEntity.ok(ApiResponse.ok(service.save(IntegrationConfig.ScopeType.SCHOOL, schoolId, providerCode, request, auth.getName())));
    }

    /** Recent activity feed (tests, live actions, webhooks) for one integration — the
     * "transaction/synchronisation history" and part of the audit trail shown on its setup
     * screen. */
    @GetMapping("/api/schools/{schoolId}/integration-configs/{providerCode}/events")
    public ResponseEntity<ApiResponse<List<IntegrationEventView>>> eventsForSchool(
            @PathVariable String schoolId, @PathVariable String providerCode, Authentication auth) {
        RoleGuard.requireSchoolAccountManager(auth);
        return ResponseEntity.ok(ApiResponse.ok(service.recentEvents(IntegrationConfig.ScopeType.SCHOOL, schoolId, providerCode)));
    }

    @GetMapping("/api/platform/integration-configs/{providerCode}/events")
    public ResponseEntity<ApiResponse<List<IntegrationEventView>>> eventsForPlatform(
            @PathVariable String providerCode, Authentication auth) {
        RoleGuard.requireSuperAdmin(auth);
        return ResponseEntity.ok(ApiResponse.ok(service.recentEvents(IntegrationConfig.ScopeType.PLATFORM, IntegrationConfig.PLATFORM_SCOPE_SCHOOL_ID, providerCode)));
    }

    /** Real file upload for a certificate-type credential (Power BI's certificate, ECZ's
     * certificate/private key) — the file's bytes are base64-encoded and stored inside the same
     * encrypted credentials blob as every other secret, so it gets identical at-rest protection;
     * this is a genuine multipart upload interaction rather than requiring the admin to open the
     * file and paste its contents by hand. */
    @PostMapping("/api/schools/{schoolId}/integration-configs/{providerCode}/credential-file")
    public ResponseEntity<ApiResponse<IntegrationConfigView>> uploadCredentialFileForSchool(
            @PathVariable String schoolId, @PathVariable String providerCode,
            @RequestParam String fieldKey, @RequestParam("file") MultipartFile file, Authentication auth) {
        RoleGuard.requireSchoolAccountManager(auth);
        return ResponseEntity.ok(ApiResponse.ok(service.saveCredentialFile(
                IntegrationConfig.ScopeType.SCHOOL, schoolId, providerCode, fieldKey, encode(file), auth.getName())));
    }

    @PostMapping("/api/platform/integration-configs/{providerCode}/credential-file")
    public ResponseEntity<ApiResponse<IntegrationConfigView>> uploadCredentialFileForPlatform(
            @PathVariable String providerCode, @RequestParam String fieldKey, @RequestParam("file") MultipartFile file, Authentication auth) {
        RoleGuard.requireSuperAdmin(auth);
        return ResponseEntity.ok(ApiResponse.ok(service.saveCredentialFile(
                IntegrationConfig.ScopeType.PLATFORM, IntegrationConfig.PLATFORM_SCOPE_SCHOOL_ID, providerCode, fieldKey, encode(file), auth.getName())));
    }

    private static String encode(MultipartFile file) {
        try {
            return Base64.getEncoder().encodeToString(file.getBytes());
        } catch (Exception e) {
            throw new BusinessException("Could not read the uploaded file: " + e.getMessage());
        }
    }
}
