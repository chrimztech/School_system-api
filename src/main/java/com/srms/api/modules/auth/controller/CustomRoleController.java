package com.srms.api.modules.auth.controller;

import com.srms.api.common.ApiResponse;
import com.srms.api.modules.auth.entity.CustomRole;
import com.srms.api.modules.auth.entity.CustomRolePermission;
import com.srms.api.modules.auth.service.RoleService;
import com.srms.api.security.RoleGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/schools/{schoolId}/roles")
@RequiredArgsConstructor
public class CustomRoleController {

    private final RoleService roleService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<CustomRole>>> list(
            @PathVariable String schoolId, Authentication auth) {
        RoleGuard.requireSchoolAccountManager(auth);
        return ResponseEntity.ok(ApiResponse.ok(roleService.listCustomRoles(schoolId)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CustomRole>> create(
            @PathVariable String schoolId,
            @RequestBody CustomRole dto,
            Authentication auth) {
        RoleGuard.requireSchoolAccountManager(auth);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(roleService.createCustomRole(schoolId, dto)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CustomRole>> update(
            @PathVariable String schoolId,
            @PathVariable String id,
            @RequestBody CustomRole dto,
            Authentication auth) {
        RoleGuard.requireSchoolAccountManager(auth);
        return ResponseEntity.ok(ApiResponse.ok(roleService.updateCustomRole(id, schoolId, dto)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable String schoolId,
            @PathVariable String id,
            Authentication auth) {
        RoleGuard.requireSchoolAccountManager(auth);
        roleService.deleteCustomRole(id, schoolId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @GetMapping("/{roleName}/permissions")
    public ResponseEntity<ApiResponse<List<CustomRolePermission>>> getPermissions(
            @PathVariable String schoolId,
            @PathVariable String roleName,
            Authentication auth) {
        RoleGuard.requireSchoolAccountManager(auth);
        return ResponseEntity.ok(ApiResponse.ok(roleService.getPermissions(schoolId, roleName)));
    }

    @PutMapping("/{roleName}/permissions")
    public ResponseEntity<ApiResponse<List<CustomRolePermission>>> savePermissions(
            @PathVariable String schoolId,
            @PathVariable String roleName,
            @RequestBody List<Map<String, String>> permissions,
            Authentication auth) {
        RoleGuard.requireSchoolAccountManager(auth);
        return ResponseEntity.ok(ApiResponse.ok(roleService.savePermissions(schoolId, roleName, permissions)));
    }
}
