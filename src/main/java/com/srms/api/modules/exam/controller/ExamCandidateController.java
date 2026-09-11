package com.srms.api.modules.exam.controller;

import com.srms.api.common.ApiResponse;
import com.srms.api.exception.ForbiddenException;
import com.srms.api.modules.exam.entity.ExamCandidate;
import com.srms.api.modules.exam.service.ExamCandidateService;
import com.srms.api.security.ModuleAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

/** Same ACADEMIC_OPERATIONS_ROLES gate as ExamController — see its javadoc. Candidate lists
 * name specific pupils and add/remove change who's actually registered for an exam, so this
 * needs the same server-side enforcement the frontend route policy only pretends to provide. */
@RestController
@RequestMapping("/api/schools/{schoolId}/exams/{examId}/candidates")
@RequiredArgsConstructor
public class ExamCandidateController {
    private final ExamCandidateService service;
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
    public ResponseEntity<ApiResponse<List<ExamCandidate>>> list(@PathVariable String schoolId, @PathVariable String examId, Authentication auth) {
        requireAcademicOperations(schoolId, auth);
        return ResponseEntity.ok(ApiResponse.ok(service.list(schoolId, examId)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ExamCandidate>> addStudent(@PathVariable String schoolId, @PathVariable String examId, @RequestBody Map<String, String> body, Authentication auth) {
        requireAcademicOperations(schoolId, auth);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(service.addStudent(schoolId, examId, body.get("studentId"))));
    }

    @PostMapping("/from-class")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> addFromClass(@PathVariable String schoolId, @PathVariable String examId, @RequestBody Map<String, String> body, Authentication auth) {
        requireAcademicOperations(schoolId, auth);
        return ResponseEntity.ok(ApiResponse.ok(service.addFromClass(schoolId, examId, body.get("classId"))));
    }

    @PostMapping("/gce")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> addGceCandidates(@PathVariable String schoolId, @PathVariable String examId, @RequestBody Map<String, List<String>> body, Authentication auth) {
        requireAcademicOperations(schoolId, auth);
        return ResponseEntity.ok(ApiResponse.ok(service.addGceCandidates(schoolId, examId, body.getOrDefault("gceCandidateIds", List.of()))));
    }

    @DeleteMapping("/{candidateId}")
    public ResponseEntity<ApiResponse<Void>> remove(@PathVariable String schoolId, @PathVariable String examId, @PathVariable String candidateId, Authentication auth) {
        requireAcademicOperations(schoolId, auth);
        service.remove(schoolId, examId, candidateId);
        return ResponseEntity.ok(ApiResponse.ok("Removed", null));
    }
}
