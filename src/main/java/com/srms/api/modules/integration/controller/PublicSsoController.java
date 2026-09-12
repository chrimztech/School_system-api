package com.srms.api.modules.integration.controller;

import com.srms.api.common.ApiResponse;
import com.srms.api.modules.integration.client.GoogleWorkspaceClient;
import com.srms.api.modules.integration.service.IntegrationConfigService;
import com.srms.api.security.tenant.TenantRequestAttributes;
import com.srms.api.security.tenant.TenantResolution;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Unauthenticated (the login page hasn't authenticated anyone yet) — hands back this school's
 * configured Google OAuth Client ID, if any, so the login page knows whether/how to render a
 * "Sign in with Google" button. A Client ID is meant to be public (it's embedded in every
 * Google-integrated frontend's JS by design), unlike the Client Secret, which never leaves the
 * backend.
 */
@RestController
@RequestMapping("/api/public/sso")
@RequiredArgsConstructor
public class PublicSsoController {
    private final IntegrationConfigService integrationConfigService;

    @GetMapping("/google-client-id")
    public ResponseEntity<ApiResponse<Map<String, String>>> googleClientId(HttpServletRequest request) {
        TenantResolution tenant = TenantRequestAttributes.resolution(request);
        String clientId = tenant.isTenant()
                ? integrationConfigService.resolveConfig(GoogleWorkspaceClient.CODE, tenant.schoolId(), "oauthClientId")
                        .map(String::valueOf)
                        .orElse(null)
                : null;
        return ResponseEntity.ok(ApiResponse.ok(Map.of("clientId", clientId == null ? "" : clientId)));
    }
}
