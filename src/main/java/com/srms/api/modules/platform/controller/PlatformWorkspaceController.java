package com.srms.api.modules.platform.controller;

import com.srms.api.common.ApiResponse;
import com.srms.api.modules.platform.dto.PlatformWorkspaceDto;
import com.srms.api.modules.platform.service.PlatformWorkspaceService;
import com.srms.api.security.RoleGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// This single blob backs every platform-console page (Platform Ops, Tenant Lifecycle/Success,
// Plan Catalog, Revenue Ops, Contract Center, Partner Management, Approval Center, Support
// Desk, Platform Config/Audit, Data Governance, Status Center, Developer Console) — including
// developer API keys, contracts, revenue cases, and platform security settings. It had no
// authorization at all: any authenticated user, of any role at any school, could read or
// overwrite the entire thing.
@RestController
@RequestMapping("/api/platform/workspace")
@RequiredArgsConstructor
public class PlatformWorkspaceController {
    private final PlatformWorkspaceService platformWorkspaceService;

    @GetMapping
    public ResponseEntity<ApiResponse<PlatformWorkspaceDto>> getWorkspace(Authentication auth) {
        RoleGuard.requireSuperAdmin(auth);
        return ResponseEntity.ok(ApiResponse.ok(platformWorkspaceService.getWorkspace()));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<PlatformWorkspaceDto>> updateWorkspace(@RequestBody PlatformWorkspaceDto dto, Authentication auth) {
        RoleGuard.requireSuperAdmin(auth);
        return ResponseEntity.ok(ApiResponse.ok(platformWorkspaceService.updateWorkspace(dto)));
    }
}
