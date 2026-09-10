package com.srms.api.modules.welfare.controller;

import com.srms.api.common.ApiResponse;
import com.srms.api.exception.ForbiddenException;
import com.srms.api.modules.welfare.entity.CounselingSession;
import com.srms.api.modules.welfare.entity.WelfareCase;
import com.srms.api.modules.welfare.service.WelfareService;
import com.srms.api.security.ModuleAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Set;

/** Welfare cases and counseling sessions are staff-only end to end (student-welfare is "false"
 * for PARENT in the frontend's own access matrix, and unlike discipline/health there's no
 * per-student lookup here for a parent to legitimately use — see StudentService's
 * deletePermanently javadoc on why: these reference a pupil by free-typed name, not studentId,
 * so there's no reliable per-pupil scoping to offer even if we wanted to). The matrix has no
 * read-only tier for this module — TEACHER, HOD, CAREER_GUIDANCE and leadership all get full
 * read/write; FINANCE and PARENT get none. */
@RestController @RequestMapping("/api/schools/{schoolId}/welfare") @RequiredArgsConstructor
public class WelfareController {
    private final WelfareService welfareService;
    private final ModuleAccessService moduleAccessService;

    private static final Set<String> FULL_ROLES = Set.of(
            "SUPER_ADMIN", "SCHOOL_ADMIN", "PRINCIPAL", "DEPUTY_HEAD", "TEACHER", "HOD", "CAREER_GUIDANCE");

    private static String roleOf(Authentication auth) {
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst().map(a -> a.replaceFirst("^ROLE_", "")).orElse("");
    }

    private void requireFull(String schoolId, Authentication auth) {
        if (!moduleAccessService.isAllowed(schoolId, auth, "student-welfare", "full", FULL_ROLES.contains(roleOf(auth)))) {
            throw new ForbiddenException("Your role cannot access welfare records");
        }
    }

    @GetMapping("/cases") public ResponseEntity<ApiResponse<List<WelfareCase>>> listCases(@PathVariable String schoolId, Authentication auth) { requireFull(schoolId, auth); return ResponseEntity.ok(ApiResponse.ok(welfareService.listCases(schoolId))); }
    @PostMapping("/cases") public ResponseEntity<ApiResponse<WelfareCase>> createCase(@PathVariable String schoolId, @RequestBody WelfareCase c, Authentication auth) { requireFull(schoolId, auth); return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(welfareService.createCase(schoolId, c))); }
    @PutMapping("/cases/{id}") public ResponseEntity<ApiResponse<WelfareCase>> updateCase(@PathVariable String schoolId, @PathVariable String id, @RequestBody WelfareCase c, Authentication auth) { requireFull(schoolId, auth); return ResponseEntity.ok(ApiResponse.ok(welfareService.updateCase(schoolId, id, c))); }

    @GetMapping("/sessions") public ResponseEntity<ApiResponse<List<CounselingSession>>> listSessions(@PathVariable String schoolId, Authentication auth) { requireFull(schoolId, auth); return ResponseEntity.ok(ApiResponse.ok(welfareService.listSessions(schoolId))); }
    @PostMapping("/sessions") public ResponseEntity<ApiResponse<CounselingSession>> createSession(@PathVariable String schoolId, @RequestBody CounselingSession s, Authentication auth) { requireFull(schoolId, auth); return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(welfareService.createSession(schoolId, s))); }
}
