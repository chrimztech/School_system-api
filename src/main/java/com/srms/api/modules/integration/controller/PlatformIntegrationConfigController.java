package com.srms.api.modules.integration.controller;

import com.srms.api.common.ApiResponse;
import com.srms.api.modules.integration.dto.PlatformIntegrationConfigDto;
import com.srms.api.modules.integration.service.PlatformIntegrationConfigService;
import com.srms.api.security.RoleGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Platform-level (not per-school) integration credentials — the payment gateway and bulk-SMS
 * provider used by every school, configured here instead of an environment-variable redeploy.
 * Super-admin only: these are shared secrets that affect every tenant on the platform. */
@RestController
@RequestMapping("/api/platform/integrations")
@RequiredArgsConstructor
public class PlatformIntegrationConfigController {
    private final PlatformIntegrationConfigService service;

    @GetMapping
    public ResponseEntity<ApiResponse<List<PlatformIntegrationConfigDto>>> list(Authentication auth) {
        RoleGuard.requireSuperAdmin(auth);
        return ResponseEntity.ok(ApiResponse.ok(service.listMasked()));
    }

    @PutMapping("/{provider}")
    public ResponseEntity<ApiResponse<PlatformIntegrationConfigDto>> update(
            @PathVariable String provider, @RequestBody PlatformIntegrationConfigDto dto, Authentication auth) {
        RoleGuard.requireSuperAdmin(auth);
        return ResponseEntity.ok(ApiResponse.ok(service.save(provider.toUpperCase(), dto)));
    }
}
