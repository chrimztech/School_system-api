package com.srms.api.modules.bursary.controller;

import com.srms.api.common.ApiResponse;
import com.srms.api.exception.ForbiddenException;
import com.srms.api.modules.bursary.entity.BursaryApplication;
import com.srms.api.modules.bursary.entity.BursaryAward;
import com.srms.api.modules.bursary.entity.BursaryRenewal;
import com.srms.api.modules.bursary.service.BursaryService;
import com.srms.api.security.ModuleAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Set;

/** Bursary awards/applications/renewals name specific pupils and their financial-aid status —
 * a socially sensitive category of data — and "bursaries" is "false" for PARENT/TEACHER/HOD/
 * CAREER_GUIDANCE in the frontend's own access matrix (only FINANCE and leadership get it).
 * There's no per-student angle to carve a parent exception out of here, unlike fees. */
@RestController @RequestMapping("/api/schools/{schoolId}/bursaries") @RequiredArgsConstructor
public class BursaryController {
    private final BursaryService bursaryService;
    private final ModuleAccessService moduleAccessService;

    private static final Set<String> FULL_ROLES = Set.of(
            "SUPER_ADMIN", "SCHOOL_ADMIN", "PRINCIPAL", "DEPUTY_HEAD", "FINANCE");

    private void requireFull(String schoolId, Authentication auth) {
        String role = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst().map(a -> a.replaceFirst("^ROLE_", "")).orElse("");
        if (!moduleAccessService.isAllowed(schoolId, auth, "bursaries", "full", FULL_ROLES.contains(role))) {
            throw new ForbiddenException("Your role cannot access bursary records");
        }
    }

    @GetMapping public ResponseEntity<ApiResponse<List<BursaryAward>>> list(@PathVariable String schoolId, Authentication auth) { requireFull(schoolId, auth); return ResponseEntity.ok(ApiResponse.ok(bursaryService.list(schoolId))); }
    @PostMapping public ResponseEntity<ApiResponse<BursaryAward>> create(@PathVariable String schoolId, @RequestBody BursaryAward b, Authentication auth) { requireFull(schoolId, auth); return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(bursaryService.create(schoolId, b))); }
    @PutMapping("/{id}") public ResponseEntity<ApiResponse<BursaryAward>> update(@PathVariable String schoolId, @PathVariable String id, @RequestBody BursaryAward b, Authentication auth) { requireFull(schoolId, auth); return ResponseEntity.ok(ApiResponse.ok(bursaryService.update(schoolId, id, b))); }

    @GetMapping("/applications") public ResponseEntity<ApiResponse<List<BursaryApplication>>> applications(@PathVariable String schoolId, Authentication auth) { requireFull(schoolId, auth); return ResponseEntity.ok(ApiResponse.ok(bursaryService.applications(schoolId))); }
    @PostMapping("/applications") public ResponseEntity<ApiResponse<BursaryApplication>> createApplication(@PathVariable String schoolId, @RequestBody BursaryApplication application, Authentication auth) { requireFull(schoolId, auth); return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(bursaryService.createApplication(schoolId, application))); }
    @PatchMapping("/applications/{id}") public ResponseEntity<ApiResponse<BursaryApplication>> updateApplication(@PathVariable String schoolId, @PathVariable String id, @RequestBody BursaryApplication patch, Authentication auth) { requireFull(schoolId, auth); return ResponseEntity.ok(ApiResponse.ok(bursaryService.updateApplication(schoolId, id, patch))); }

    @GetMapping("/renewals") public ResponseEntity<ApiResponse<List<BursaryRenewal>>> renewals(@PathVariable String schoolId, Authentication auth) { requireFull(schoolId, auth); return ResponseEntity.ok(ApiResponse.ok(bursaryService.renewals(schoolId))); }
    @PostMapping("/renewals") public ResponseEntity<ApiResponse<BursaryRenewal>> createRenewal(@PathVariable String schoolId, @RequestBody BursaryRenewal renewal, Authentication auth) { requireFull(schoolId, auth); return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(bursaryService.createRenewal(schoolId, renewal))); }
    @PatchMapping("/renewals/{id}") public ResponseEntity<ApiResponse<BursaryRenewal>> updateRenewal(@PathVariable String schoolId, @PathVariable String id, @RequestBody BursaryRenewal patch, Authentication auth) { requireFull(schoolId, auth); return ResponseEntity.ok(ApiResponse.ok(bursaryService.updateRenewal(schoolId, id, patch))); }
}
