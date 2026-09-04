package com.srms.api.security;

import com.srms.api.exception.ForbiddenException;
import com.srms.api.modules.auth.entity.CustomRolePermission;
import com.srms.api.modules.auth.repository.CustomRolePermissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Lets a super admin grant or revoke a system role's (TEACHER, HOD, FINANCE, ...) access to a
 * module on a per-school basis, on top of the hardcoded baseline every controller already
 * enforces. Every one of this class's checks is additive-safe by construction: when no override
 * row exists for a given school/role/module, behavior is byte-for-byte what it was before this
 * class existed — the caller's own hardcoded default decides. An override only ever changes the
 * outcome when a super admin has explicitly set one via the System role permissions page.
 *
 * Overrides are stored in the same {@code custom_role_permissions} table as custom-role
 * permissions (see {@link com.srms.api.modules.auth.service.RoleService}), namespaced under a
 * "sysrole:" role_name prefix so the two concepts never collide.
 */
@Service
@RequiredArgsConstructor
public class ModuleAccessService {
    private static final String SYSTEM_ROLE_PREFIX = "sysrole:";
    private static final Map<String, Integer> LEVEL_RANK = Map.of("none", 0, "read", 1, "full", 2);

    private final CustomRolePermissionRepository permissionRepository;

    /**
     * Looks up whether a super admin has set an explicit override for this school/role/module.
     * Returns {@code null} when no override exists — the caller must fall back to its own
     * hardcoded default check in that case. Super admin itself is never subject to an override
     * (it always has full access) so the platform admin can never be locked out by a mistaken
     * setting.
     */
    public Boolean checkOverride(String schoolId, String role, String module, String minLevel) {
        if (role == null || module == null) return null;
        if ("SUPER_ADMIN".equalsIgnoreCase(role)) return true;
        return permissionRepository
                .findBySchoolIdAndRoleNameAndModule(schoolId, SYSTEM_ROLE_PREFIX + role.toLowerCase(), module)
                .map(CustomRolePermission::getAccess)
                .map(access -> LEVEL_RANK.getOrDefault(access, 0) >= LEVEL_RANK.getOrDefault(minLevel, 2))
                .orElse(null);
    }

    /** {@code defaultAllowed} is whatever the caller's own hardcoded rule already computed —
     * an explicit override replaces that outcome; the absence of one leaves it untouched. */
    public boolean isAllowed(String schoolId, Authentication auth, String module, String minLevel, boolean defaultAllowed) {
        Boolean override = checkOverride(schoolId, RoleGuard.roleOf(auth), module, minLevel);
        return override != null ? override : defaultAllowed;
    }

    public void require(String schoolId, Authentication auth, String module, String minLevel, boolean defaultAllowed) {
        if (!isAllowed(schoolId, auth, module, minLevel, defaultAllowed)) {
            throw new ForbiddenException("Your role does not have permission to access this");
        }
    }
}
