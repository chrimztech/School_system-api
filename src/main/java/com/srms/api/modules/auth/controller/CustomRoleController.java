package com.srms.api.modules.auth.controller;

import com.srms.api.common.ApiResponse;
import com.srms.api.exception.ForbiddenException;
import com.srms.api.modules.auth.entity.CustomRole;
import com.srms.api.modules.auth.entity.CustomRolePermission;
import com.srms.api.modules.auth.service.RoleService;
import com.srms.api.security.ModuleAccessService;
import com.srms.api.security.RoleGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/schools/{schoolId}/roles")
@RequiredArgsConstructor
public class CustomRoleController {

    private final RoleService roleService;
    private final ModuleAccessService moduleAccessService;

    // Mirrors RoleGuard's own (private) SCHOOL_ACCOUNT_MANAGERS set — reconstructed here so the
    // original hardcoded default can be passed through as the isAllowed() defaultAllowed
    // argument without modifying RoleGuard itself.
    private static final Set<String> SCHOOL_ACCOUNT_MANAGERS = Set.of(
            "SUPER_ADMIN", "SCHOOL_ADMIN", "PRINCIPAL", "DEPUTY_HEAD");

    @GetMapping
    public ResponseEntity<ApiResponse<List<CustomRole>>> list(
            @PathVariable String schoolId, Authentication auth) {
        requireManage(schoolId, auth);
        return ResponseEntity.ok(ApiResponse.ok(roleService.listCustomRoles(schoolId)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CustomRole>> create(
            @PathVariable String schoolId,
            @RequestBody CustomRole dto,
            Authentication auth) {
        requireManage(schoolId, auth);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(roleService.createCustomRole(schoolId, dto)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CustomRole>> update(
            @PathVariable String schoolId,
            @PathVariable String id,
            @RequestBody CustomRole dto,
            Authentication auth) {
        requireManage(schoolId, auth);
        return ResponseEntity.ok(ApiResponse.ok(roleService.updateCustomRole(id, schoolId, dto)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable String schoolId,
            @PathVariable String id,
            Authentication auth) {
        requireManage(schoolId, auth);
        roleService.deleteCustomRole(id, schoolId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @GetMapping("/{roleName}/permissions")
    public ResponseEntity<ApiResponse<List<CustomRolePermission>>> getPermissions(
            @PathVariable String schoolId,
            @PathVariable String roleName,
            Authentication auth) {
        requireManage(schoolId, auth);
        return ResponseEntity.ok(ApiResponse.ok(roleService.getPermissions(schoolId, roleName)));
    }

    @PutMapping("/{roleName}/permissions")
    public ResponseEntity<ApiResponse<List<CustomRolePermission>>> savePermissions(
            @PathVariable String schoolId,
            @PathVariable String roleName,
            @RequestBody List<Map<String, String>> permissions,
            Authentication auth) {
        requireManage(schoolId, auth);
        return ResponseEntity.ok(ApiResponse.ok(roleService.savePermissions(schoolId, roleName, permissions)));
    }

    private void requireManage(String schoolId, Authentication auth) {
        if (!moduleAccessService.isAllowed(schoolId, auth, "access", "full", SCHOOL_ACCOUNT_MANAGERS.contains(RoleGuard.roleOf(auth)))) {
            throw new ForbiddenException("Your role cannot manage school accounts or permissions");
        }
    }
}
