package com.srms.api.security.tenant;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class TenantAccessFilter extends OncePerRequestFilter {
    private static final Pattern SCHOOL_PATH = Pattern.compile("^/api/schools/([^/]+)(?:/.*)?$");
    private static final Pattern PUBLIC_SCHOOL_PATH = Pattern.compile("^/api/public/schools/by-slug/([^/]+)$");
    private final TenantResolver tenantResolver;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        TenantResolution resolution = tenantResolver.resolve(request.getServerName()).orElse(null);
        if (resolution == null) {
            writeError(response, HttpServletResponse.SC_NOT_FOUND, "School not found");
            return;
        }
        request.setAttribute(TenantRequestAttributes.RESOLUTION, resolution);

        String path = request.getRequestURI();
        String pathSchoolId = pathSchoolId(path);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean authenticated = authentication != null && authentication.isAuthenticated();
        boolean superAdmin = authenticated && hasRole(authentication, "SUPER_ADMIN");
        String actorSchoolId = authenticated && authentication.getCredentials() != null
                ? authentication.getCredentials().toString()
                : null;

        if (resolution.isTenant()) {
            if (path.startsWith("/api/platform/") || path.startsWith("/api/admin/")) {
                writeError(response, HttpServletResponse.SC_NOT_FOUND, "Not found");
                return;
            }
            Matcher publicSchool = PUBLIC_SCHOOL_PATH.matcher(path);
            if (publicSchool.matches() && !resolution.slug().equalsIgnoreCase(publicSchool.group(1))) {
                writeError(response, HttpServletResponse.SC_NOT_FOUND, "School not found");
                return;
            }
            if (pathSchoolId != null && !resolution.schoolId().equals(pathSchoolId)) {
                writeError(response, HttpServletResponse.SC_FORBIDDEN, "Cross-school access denied");
                return;
            }
            if (authenticated && (superAdmin || !resolution.schoolId().equals(actorSchoolId))) {
                writeError(response, HttpServletResponse.SC_FORBIDDEN, "Cross-school access denied");
                return;
            }
            if (!headersAgree(request, resolution)) {
                writeError(response, HttpServletResponse.SC_FORBIDDEN, "Cross-school access denied");
                return;
            }
            request.setAttribute(TenantRequestAttributes.EXPECTED_SCHOOL_ID, resolution.schoolId());
        } else if (resolution.scope() == TenantResolution.Scope.PLATFORM) {
            if (authenticated && !superAdmin) {
                writeError(response, HttpServletResponse.SC_FORBIDDEN, "School accounts must use their school subdomain");
                return;
            }
        } else {
            if (authenticated && !superAdmin && pathSchoolId != null && !pathSchoolId.equals(actorSchoolId)) {
                writeError(response, HttpServletResponse.SC_FORBIDDEN, "Cross-school access denied");
                return;
            }
            String expectedSchoolId = pathSchoolId != null ? pathSchoolId : actorSchoolId;
            if (expectedSchoolId != null) {
                request.setAttribute(TenantRequestAttributes.EXPECTED_SCHOOL_ID, expectedSchoolId);
            }
        }

        chain.doFilter(request, response);
    }

    private static String pathSchoolId(String path) {
        Matcher matcher = SCHOOL_PATH.matcher(path);
        return matcher.matches() ? matcher.group(1) : null;
    }

    private static boolean headersAgree(HttpServletRequest request, TenantResolution resolution) {
        String schoolId = firstHeader(request, "X-School-Id", "X-Tenant-Id");
        String schoolSlug = request.getHeader("X-School-Slug");
        return (schoolId == null || schoolId.equals(resolution.schoolId()))
                && (schoolSlug == null || schoolSlug.equalsIgnoreCase(resolution.slug()));
    }

    private static String firstHeader(HttpServletRequest request, String... names) {
        for (String name : names) {
            String value = request.getHeader(name);
            if (value != null && !value.isBlank()) return value.trim();
        }
        return null;
    }

    private static boolean hasRole(Authentication authentication, String role) {
        String authority = "ROLE_" + role;
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority::equals);
    }

    private static void writeError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"success\":false,\"message\":\"" + message + "\",\"data\":null}");
    }
}
