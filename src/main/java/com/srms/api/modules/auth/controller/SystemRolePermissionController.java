package com.srms.api.modules.auth.controller;

import com.srms.api.common.ApiResponse;
import com.srms.api.exception.BusinessException;
import com.srms.api.exception.ForbiddenException;
import com.srms.api.modules.auth.entity.CustomRolePermission;
import com.srms.api.modules.auth.service.RoleService;
import com.srms.api.security.RoleGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Lets school leadership grant or revoke a built-in system role's (TEACHER, HOD, FINANCE, ...)
 * access to a module, per school — the same capability the Users & Roles page already gives
 * for custom roles, extended to the roles every school actually has real users in. Reads are
 * open to school leadership (so a school admin can see what's been set for their own school);
 * writes are open to school leadership too, EXCEPT for the leadership roles themselves
 * (SCHOOL_ADMIN, PRINCIPAL, DEPUTY_HEAD) — a school admin editing that ceiling upward would be
 * a self-privilege-escalation path (any current or future leadership account at that school
 * would inherit the raised ceiling), so those stay super-admin-only.
 */
@RestController
@RequestMapping("/api/schools/{schoolId}/system-roles/{role}/permissions")
@RequiredArgsConstructor
public class SystemRolePermissionController {
    private final RoleService roleService;

    // Mirrors the frontend's Role union (src/lib/auth.tsx) minus super_admin, which is never
    // subject to an override (see ModuleAccessService.checkOverride).
    private static final Set<String> SYSTEM_ROLES = Set.of(
            "school_admin", "teacher", "hod", "finance", "parent", "principal", "deputy_head", "career_guidance");

    // Kept in sync with the frontend's SCHOOL_LEADERSHIP_ROLES (src/lib/auth.tsx) — the roles
    // only a super admin may override, to close the self-privilege-escalation path above.
    private static final Set<String> LEADERSHIP_ROLES = Set.of("school_admin", "principal", "deputy_head");

    @GetMapping
    public ResponseEntity<ApiResponse<List<CustomRolePermission>>> get(
            @PathVariable String schoolId, @PathVariable String role, Authentication auth) {
        RoleGuard.requireSchoolAccountManager(auth);
        validateRole(role);
        return ResponseEntity.ok(ApiResponse.ok(roleService.getSystemRolePermissions(schoolId, role)));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<List<CustomRolePermission>>> save(
            @PathVariable String schoolId, @PathVariable String role,
            @RequestBody List<Map<String, String>> permissions, Authentication auth) {
        RoleGuard.requireSchoolAccountManager(auth);
        validateRole(role);
        if (!RoleGuard.isSuperAdmin(auth) && LEADERSHIP_ROLES.contains(role.toLowerCase())) {
            throw new ForbiddenException("Only the system administrator can change permissions for a leadership role");
        }
        return ResponseEntity.ok(ApiResponse.ok(roleService.saveSystemRolePermissions(schoolId, role, permissions)));
    }

    private void validateRole(String role) {
        if (!SYSTEM_ROLES.contains(role.toLowerCase())) {
            throw new BusinessException("Unrecognized system role: \"" + role + "\"");
        }
    }
}
