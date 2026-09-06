package com.srms.api.modules.school.controller;

import com.srms.api.common.ApiResponse;
import com.srms.api.exception.ForbiddenException;
import com.srms.api.modules.school.dto.SchoolDto;
import com.srms.api.modules.school.service.SchoolService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class SchoolController {
    private final SchoolService schoolService;

    // ── Public (no auth) ────────────────────────────────────────────────────
    @GetMapping("/api/public/schools/by-slug/{slug}")
    public ResponseEntity<?> getBySlug(@PathVariable String slug) {
        return schoolService.findPublicBySlug(slug)
                .<ResponseEntity<?>>map(summary -> ResponseEntity.ok(ApiResponse.ok(summary)))
                .orElseGet(() -> ResponseEntity.status(404).body(ApiResponse.error("School not found")));
    }

    // ── Authenticated ────────────────────────────────────────────────────────
    @GetMapping("/api/schools")
    public ResponseEntity<ApiResponse<List<SchoolDto>>> getAll(Authentication auth) {
        if ("SUPER_ADMIN".equals(roleOf(auth))) {
            return ResponseEntity.ok(ApiResponse.ok(schoolService.findAll()));
        }
        String schoolId = actorSchool(auth);
        if (schoolId == null) throw new ForbiddenException("Your account is not assigned to a school");
        return ResponseEntity.ok(ApiResponse.ok(List.of(schoolService.findById(schoolId))));
    }

    @GetMapping("/api/schools/{id}")
    public ResponseEntity<ApiResponse<SchoolDto>> getById(@PathVariable String id, Authentication auth) {
        if (!"SUPER_ADMIN".equals(roleOf(auth)) && !id.equals(actorSchool(auth))) {
            throw new ForbiddenException("You cannot access another school's settings");
        }
        return ResponseEntity.ok(ApiResponse.ok(schoolService.findById(id)));
    }

    // Split from getById() on purpose — see SchoolBrandingAsset's javadoc. Only the report
    // card and the Settings branding form need to call this; the tenant-context fetch every
    // page load runs (getById/findAll) never does.
    @GetMapping("/api/schools/{id}/branding-assets")
    public ResponseEntity<ApiResponse<SchoolDto>> getBrandingAssets(@PathVariable String id, Authentication auth) {
        if (!"SUPER_ADMIN".equals(roleOf(auth)) && !id.equals(actorSchool(auth))) {
            throw new ForbiddenException("You cannot access another school's settings");
        }
        return ResponseEntity.ok(ApiResponse.ok(schoolService.findBrandingAssets(id)));
    }

    @PostMapping("/api/schools")
    public ResponseEntity<ApiResponse<SchoolDto>> create(@RequestBody SchoolDto dto, Authentication auth) {
        if (!"SUPER_ADMIN".equals(roleOf(auth))) throw new ForbiddenException("Only the system administrator can onboard schools");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(schoolService.create(dto)));
    }

    @PutMapping("/api/schools/{id}")
    public ResponseEntity<ApiResponse<SchoolDto>> update(@PathVariable String id, @RequestBody SchoolDto dto, Authentication auth) {
        String role = roleOf(auth);
        String actorSchool = actorSchool(auth);
        boolean schoolLeader = "SCHOOL_ADMIN".equals(role) || "PRINCIPAL".equals(role) || "DEPUTY_HEAD".equals(role);
        if (!"SUPER_ADMIN".equals(role) && (!schoolLeader || !id.equals(actorSchool))) {
            throw new ForbiddenException("You cannot change another school's settings");
        }
        if (!"SUPER_ADMIN".equals(role) && !"SCHOOL_ADMIN".equals(role)) {
            dto.setGradingScale(null);
            dto.setResultPublicationMode(null);
            dto.setGradingBands(null);
            dto.setPassMark(null);
        }
        return ResponseEntity.ok(ApiResponse.ok(schoolService.update(id, dto)));
    }

    @DeleteMapping("/api/schools/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String id, Authentication auth) {
        if (!"SUPER_ADMIN".equals(roleOf(auth))) throw new ForbiddenException("Only the system administrator can delete schools");
        schoolService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("School permanently deleted", null));
    }

    private static String roleOf(Authentication auth) {
        return auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).findFirst()
                .map(authority -> authority.replaceFirst("^ROLE_", "")).orElse("");
    }

    private static String actorSchool(Authentication auth) {
        return auth.getCredentials() == null ? null : auth.getCredentials().toString();
    }
}
