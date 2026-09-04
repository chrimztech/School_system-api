package com.srms.api.security;

import com.srms.api.exception.ForbiddenException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import java.util.Set;

public final class RoleGuard {
    private static final Set<String> SCHOOL_ACCOUNT_MANAGERS = Set.of(
            "SUPER_ADMIN", "SCHOOL_ADMIN", "PRINCIPAL", "DEPUTY_HEAD");

    /** Matches the frontend's own MODULE_MATRIX (auth.tsx) for "accounting", "procurement",
     * "risk-register" and "reporting" — the only roles those modules mark as full (not
     * "read"/false) access, beyond school leadership, is FINANCE. */
    private static final Set<String> FINANCE_AND_LEADERSHIP = Set.of(
            "SUPER_ADMIN", "SCHOOL_ADMIN", "PRINCIPAL", "DEPUTY_HEAD", "FINANCE");

    private RoleGuard() {}

    public static String roleOf(Authentication authentication) {
        if (authentication == null) return "";
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority.startsWith("ROLE_"))
                .map(authority -> authority.substring(5))
                .findFirst()
                .orElse("");
    }

    public static void requireSchoolAccountManager(Authentication authentication) {
        if (!SCHOOL_ACCOUNT_MANAGERS.contains(roleOf(authentication))) {
            throw new ForbiddenException("Your role cannot manage school accounts or permissions");
        }
    }

    public static void requireFinanceOrLeadership(Authentication authentication) {
        if (!FINANCE_AND_LEADERSHIP.contains(roleOf(authentication))) {
            throw new ForbiddenException("Your role cannot access this module");
        }
    }

    public static void requireSuperAdmin(Authentication authentication) {
        if (!"SUPER_ADMIN".equals(roleOf(authentication))) {
            throw new ForbiddenException("This action is restricted to the system administrator");
        }
    }

    public static boolean isSuperAdmin(Authentication authentication) {
        return "SUPER_ADMIN".equals(roleOf(authentication));
    }
}
