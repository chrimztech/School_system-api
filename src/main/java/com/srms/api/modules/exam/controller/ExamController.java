package com.srms.api.modules.exam.controller;

import com.srms.api.common.ApiResponse;
import com.srms.api.exception.ForbiddenException;
import com.srms.api.modules.exam.entity.ExamPaper;
import com.srms.api.modules.exam.service.ExamService;
import com.srms.api.security.ModuleAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Set;

/** Mirrors the frontend's ACADEMIC_OPERATIONS_ROLES (route-access.ts), which is what actually
 * keeps a parent from ever navigating to /exams — none of that stops a parent's own valid JWT
 * calling this controller directly, which had no auth check at all before this. */
@RestController @RequestMapping("/api/schools/{schoolId}/exams") @RequiredArgsConstructor
public class ExamController {
    private final ExamService examService;
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

    @GetMapping public ResponseEntity<ApiResponse<List<ExamPaper>>> list(@PathVariable String schoolId, Authentication auth) { requireAcademicOperations(schoolId, auth); return ResponseEntity.ok(ApiResponse.ok(examService.list(schoolId))); }
    @PostMapping public ResponseEntity<ApiResponse<ExamPaper>> create(@PathVariable String schoolId, @RequestBody ExamPaper paper, Authentication auth) { requireAcademicOperations(schoolId, auth); return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(examService.create(schoolId, paper))); }
    @PutMapping("/{id}") public ResponseEntity<ApiResponse<ExamPaper>> update(@PathVariable String schoolId, @PathVariable String id, @RequestBody ExamPaper paper, Authentication auth) { requireAcademicOperations(schoolId, auth); return ResponseEntity.ok(ApiResponse.ok(examService.update(schoolId, id, paper))); }
    @DeleteMapping("/{id}") public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String schoolId, @PathVariable String id, Authentication auth) { requireAcademicOperations(schoolId, auth); examService.delete(schoolId, id); return ResponseEntity.ok(ApiResponse.ok("Deleted", null)); }
}
