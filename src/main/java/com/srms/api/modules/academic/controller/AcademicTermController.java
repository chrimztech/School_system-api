package com.srms.api.modules.academic.controller;

import com.srms.api.common.ApiResponse;
import com.srms.api.exception.ForbiddenException;
import com.srms.api.modules.academic.entity.AcademicTerm;
import com.srms.api.modules.academic.service.AcademicService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/schools/{schoolId}/academic-terms")
@RequiredArgsConstructor
public class AcademicTermController {
    private final AcademicService academicService;

    private static final Set<String> CAN_MANAGE_ROLES = Set.of(
            "SUPER_ADMIN", "SCHOOL_ADMIN", "PRINCIPAL", "DEPUTY_HEAD");

    private static String roleOf(Authentication auth) {
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst().map(a -> a.replaceFirst("^ROLE_", "")).orElse("");
    }

    private void requireCanManage(Authentication auth) {
        if (!CAN_MANAGE_ROLES.contains(roleOf(auth))) {
            throw new ForbiddenException("Your role cannot manage the academic term calendar");
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<AcademicTerm>>> getAll(@PathVariable String schoolId) {
        return ResponseEntity.ok(ApiResponse.ok(academicService.findAllTerms(schoolId)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AcademicTerm>> create(@PathVariable String schoolId, @RequestBody AcademicTerm dto, Authentication auth) {
        requireCanManage(auth);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(academicService.createTerm(schoolId, dto)));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<AcademicTerm>> update(@PathVariable String schoolId, @PathVariable String id, @RequestBody AcademicTerm patch, Authentication auth) {
        requireCanManage(auth);
        return ResponseEntity.ok(ApiResponse.ok(academicService.updateTerm(id, schoolId, patch)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String schoolId, @PathVariable String id, Authentication auth) {
        requireCanManage(auth);
        academicService.deleteTerm(id, schoolId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
