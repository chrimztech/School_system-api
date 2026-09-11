package com.srms.api.modules.incident.controller;

import com.srms.api.common.ApiResponse;
import com.srms.api.exception.ForbiddenException;
import com.srms.api.modules.incident.entity.Incident;
import com.srms.api.modules.incident.service.IncidentService;
import com.srms.api.security.ModuleAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Set;

/** Role sets mirror the frontend's ACCESS matrix for "incident-management": leadership full
 * read/write, TEACHER/HOD read-only, everyone else (FINANCE, CAREER_GUIDANCE, PARENT) none. */
@RestController @RequestMapping("/api/schools/{schoolId}/incidents") @RequiredArgsConstructor
public class IncidentController {
    private final IncidentService incidentService;
    private final ModuleAccessService moduleAccessService;

    private static final Set<String> FULL_ROLES = Set.of("SUPER_ADMIN", "SCHOOL_ADMIN", "PRINCIPAL", "DEPUTY_HEAD");
    private static final Set<String> READ_ROLES = Set.of("SUPER_ADMIN", "SCHOOL_ADMIN", "PRINCIPAL", "DEPUTY_HEAD", "TEACHER", "HOD");

    private static String roleOf(Authentication auth) {
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst().map(a -> a.replaceFirst("^ROLE_", "")).orElse("");
    }

    private void requireRead(String schoolId, Authentication auth) {
        if (!moduleAccessService.isAllowed(schoolId, auth, "incident-management", "read", READ_ROLES.contains(roleOf(auth)))) {
            throw new ForbiddenException("Your role cannot access incident records");
        }
    }

    private void requireFull(String schoolId, Authentication auth) {
        if (!moduleAccessService.isAllowed(schoolId, auth, "incident-management", "full", FULL_ROLES.contains(roleOf(auth)))) {
            throw new ForbiddenException("Your role cannot record or resolve incidents");
        }
    }

    @GetMapping public ResponseEntity<ApiResponse<List<Incident>>> list(@PathVariable String schoolId, Authentication auth) { requireRead(schoolId, auth); return ResponseEntity.ok(ApiResponse.ok(incidentService.list(schoolId))); }
    @PostMapping public ResponseEntity<ApiResponse<Incident>> create(@PathVariable String schoolId, @RequestBody Incident i, Authentication auth) { requireFull(schoolId, auth); return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(incidentService.create(schoolId, i))); }
    @PutMapping("/{id}") public ResponseEntity<ApiResponse<Incident>> update(@PathVariable String schoolId, @PathVariable String id, @RequestBody Incident i, Authentication auth) { requireFull(schoolId, auth); return ResponseEntity.ok(ApiResponse.ok(incidentService.update(schoolId, id, i))); }
    @PatchMapping("/{id}/resolve") public ResponseEntity<ApiResponse<Void>> resolve(@PathVariable String schoolId, @PathVariable String id, Authentication auth) { requireFull(schoolId, auth); incidentService.resolve(schoolId, id); return ResponseEntity.ok(ApiResponse.ok("Resolved", null)); }
}
