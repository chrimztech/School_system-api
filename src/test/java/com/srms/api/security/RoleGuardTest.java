package com.srms.api.security;

import com.srms.api.exception.ForbiddenException;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RoleGuardTest {
    @Test
    void schoolLeadershipCanManageAccounts() {
        for (String role : List.of("SUPER_ADMIN", "SCHOOL_ADMIN", "PRINCIPAL", "DEPUTY_HEAD")) {
            assertThatCode(() -> RoleGuard.requireSchoolAccountManager(auth(role)))
                    .as(role)
                    .doesNotThrowAnyException();
        }
    }

    @Test
    void operationalAndParentRolesCannotManageAccounts() {
        for (String role : List.of("TEACHER", "HOD", "CAREER_GUIDANCE", "FINANCE", "PARENT")) {
            assertThatThrownBy(() -> RoleGuard.requireSchoolAccountManager(auth(role)))
                    .as(role)
                    .isInstanceOf(ForbiddenException.class);
        }
    }

    @Test
    void globalAdministrationRequiresSuperAdmin() {
        assertThatCode(() -> RoleGuard.requireSuperAdmin(auth("SUPER_ADMIN")))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> RoleGuard.requireSuperAdmin(auth("SCHOOL_ADMIN")))
                .isInstanceOf(ForbiddenException.class);
    }

    private UsernamePasswordAuthenticationToken auth(String role) {
        return new UsernamePasswordAuthenticationToken(
                "user-a", "school-a", List.of(new SimpleGrantedAuthority("ROLE_" + role)));
    }
}
