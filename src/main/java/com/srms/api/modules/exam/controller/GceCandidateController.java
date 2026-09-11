package com.srms.api.modules.exam.controller;

import com.srms.api.common.ApiResponse;
import com.srms.api.common.BulkImportResult;
import com.srms.api.exception.ForbiddenException;
import com.srms.api.modules.exam.dto.GceCandidateDto;
import com.srms.api.modules.exam.entity.GceCandidate;
import com.srms.api.modules.exam.service.GceCandidateService;
import com.srms.api.security.ModuleAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

/** Same ACADEMIC_OPERATIONS_ROLES gate as ExamController/ExamCandidateController. */
@RestController
@RequestMapping("/api/schools/{schoolId}/gce-candidates")
@RequiredArgsConstructor
public class GceCandidateController {
    private final GceCandidateService service;
    private final ModuleAccessService moduleAccessService;

    private static final Set<String> ACADEMIC_OPERATIONS_ROLES = Set.of(
            "SUPER_ADMIN", "SCHOOL_ADMIN", "PRINCIPAL", "DEPUTY_HEAD", "HOD", "CAREER_GUIDANCE", "TEACHER");

    private void requireAcademicOperations(String schoolId, Authentication auth) {
        String role = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst().map(a -> a.replaceFirst("^ROLE_", "")).orElse("");
        if (!moduleAccessService.isAllowed(schoolId, auth, "assessments", "read", ACADEMIC_OPERATIONS_ROLES.contains(role))) {
            throw new ForbiddenException("Your role cannot access examination records");
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<GceCandidate>>> list(@PathVariable String schoolId, Authentication auth) {
        requireAcademicOperations(schoolId, auth);
        return ResponseEntity.ok(ApiResponse.ok(service.list(schoolId)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<GceCandidate>> getById(@PathVariable String schoolId, @PathVariable String id, Authentication auth) {
        requireAcademicOperations(schoolId, auth);
        return ResponseEntity.ok(ApiResponse.ok(service.findById(id, schoolId)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<GceCandidate>> create(@PathVariable String schoolId, @RequestBody GceCandidateDto dto, Authentication auth) {
        requireAcademicOperations(schoolId, auth);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(service.create(schoolId, dto)));
    }

    @PostMapping("/bulk")
    public ResponseEntity<ApiResponse<BulkImportResult>> bulkCreate(@PathVariable String schoolId, @RequestBody List<GceCandidateDto> dtos, Authentication auth) {
        requireAcademicOperations(schoolId, auth);
        return ResponseEntity.ok(ApiResponse.ok(service.bulkCreate(schoolId, dtos)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<GceCandidate>> update(@PathVariable String schoolId, @PathVariable String id, @RequestBody GceCandidateDto dto, Authentication auth) {
        requireAcademicOperations(schoolId, auth);
        return ResponseEntity.ok(ApiResponse.ok(service.update(id, schoolId, dto)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String schoolId, @PathVariable String id, Authentication auth) {
        requireAcademicOperations(schoolId, auth);
        service.delete(id, schoolId);
        return ResponseEntity.ok(ApiResponse.ok("Deleted", null));
    }
}
