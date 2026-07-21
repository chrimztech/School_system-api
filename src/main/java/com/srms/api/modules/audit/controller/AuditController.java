package com.srms.api.modules.audit.controller;

import com.srms.api.common.ApiResponse;
import com.srms.api.common.PageRequestUtil;
import com.srms.api.common.PageResponse;
import com.srms.api.exception.ForbiddenException;
import com.srms.api.modules.audit.entity.AuditEvent;
import com.srms.api.modules.audit.service.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController @RequestMapping("/api/schools/{schoolId}/audit") @RequiredArgsConstructor
public class AuditController {
    private final AuditService auditService;

    private static final Set<String> ADMIN_ROLES = Set.of(
            "SUPER_ADMIN", "SCHOOL_ADMIN", "PRINCIPAL", "DEPUTY_HEAD");

    @GetMapping
    public ResponseEntity<ApiResponse<?>> getAll(
            @PathVariable String schoolId,
            @RequestParam(required = false) Integer page, @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sortBy, @RequestParam(required = false) String sortDir,
            Authentication auth) {
        assertAdmin(schoolId, auth);
        Pageable pageable = PageRequestUtil.build(page, size, sortBy, sortDir);
        if (pageable == null) return ResponseEntity.ok(ApiResponse.ok(auditService.findAll(schoolId)));
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.of(auditService.findAllPaged(schoolId, pageable))));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AuditEvent>> create(
            @PathVariable String schoolId, @RequestBody AuditEvent event, Authentication auth) {
        assertAdmin(schoolId, auth);
        // Actor/role are derived from the authenticated principal, never trusted from the
        // request body — otherwise any admin could forge audit entries attributed to someone else.
        event.setActor(auth.getName());
        event.setRole(roleOf(auth));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(auditService.create(schoolId, event)));
    }

    private static String roleOf(Authentication auth) {
        return auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).findFirst()
                .map(authority -> authority.replaceFirst("^ROLE_", "")).orElse("");
    }

    private void assertAdmin(String schoolId, Authentication auth) {
        String role = roleOf(auth);
        if (!ADMIN_ROLES.contains(role.toUpperCase())) {
            throw new ForbiddenException("Only school administrators can view the audit trail");
        }
        if ("SUPER_ADMIN".equalsIgnoreCase(role)) return;
        String actorSchool = auth.getCredentials() == null ? null : auth.getCredentials().toString();
        if (!schoolId.equals(actorSchool)) {
            throw new ForbiddenException("You cannot access another school's audit trail");
        }
    }
}
