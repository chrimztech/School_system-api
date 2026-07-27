package com.srms.api.modules.assessment.controller;
import com.srms.api.common.ApiResponse;
import com.srms.api.common.PageRequestUtil;
import com.srms.api.common.PageResponse;
import com.srms.api.modules.assessment.entity.Assessment;
import com.srms.api.modules.assessment.entity.AssessmentResult;
import com.srms.api.modules.assessment.service.AssessmentService;
import com.srms.api.exception.ForbiddenException;
import com.srms.api.modules.auth.entity.AppUser;
import com.srms.api.modules.auth.repository.UserRepository;
import com.srms.api.modules.student.entity.Student;
import com.srms.api.modules.student.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
@RestController @RequestMapping("/api/schools/{schoolId}/assessments") @RequiredArgsConstructor
public class AssessmentController {
    private final AssessmentService assessmentService;
    private final UserRepository userRepository;
    private final StudentRepository studentRepository;

    private static String roleOf(Authentication auth) {
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst().map(a -> a.replaceFirst("^ROLE_", "")).orElse("");
    }

    @GetMapping public ResponseEntity<ApiResponse<List<Assessment>>> getAll(@PathVariable String schoolId, @RequestParam(required = false) String term, @RequestParam(required = false) String academicYear, Authentication auth) { return ResponseEntity.ok(ApiResponse.ok(assessmentService.findAllForActor(schoolId, term, academicYear, auth.getName(), roleOf(auth)))); }
    @GetMapping("/{id}") public ResponseEntity<ApiResponse<Assessment>> getById(@PathVariable String schoolId, @PathVariable String id, Authentication auth) { return ResponseEntity.ok(ApiResponse.ok(assessmentService.findByIdForActor(id, schoolId, auth.getName(), roleOf(auth)))); }
    @PostMapping public ResponseEntity<ApiResponse<Assessment>> create(@PathVariable String schoolId, @RequestBody Assessment dto, Authentication auth) { return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(assessmentService.create(schoolId, dto, auth.getName(), roleOf(auth)))); }
    @PutMapping("/{id}") public ResponseEntity<ApiResponse<Assessment>> update(@PathVariable String schoolId, @PathVariable String id, @RequestBody Assessment dto, Authentication auth) { return ResponseEntity.ok(ApiResponse.ok(assessmentService.update(id, schoolId, dto, auth.getName(), roleOf(auth)))); }
    @GetMapping("/{id}/results") public ResponseEntity<ApiResponse<List<AssessmentResult>>> getResults(@PathVariable String schoolId, @PathVariable String id, Authentication auth) {
        if ("PARENT".equalsIgnoreCase(roleOf(auth))) throw new ForbiddenException("Parents can only view their child's published report card");
        return ResponseEntity.ok(ApiResponse.ok(assessmentService.getResults(id, schoolId, auth.getName(), roleOf(auth))));
    }
    @PostMapping("/{id}/results") public ResponseEntity<ApiResponse<AssessmentResult>> saveResult(@PathVariable String schoolId, @PathVariable String id, @RequestBody AssessmentResult result, Authentication auth) { result.setSchoolId(schoolId); result.setAssessmentId(id); return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(assessmentService.saveResult(result, auth.getName(), roleOf(auth)))); }
    @PostMapping("/{id}/results/bulk") public ResponseEntity<ApiResponse<List<AssessmentResult>>> saveResultsBulk(@PathVariable String schoolId, @PathVariable String id, @RequestBody List<AssessmentResult> results, Authentication auth) { return ResponseEntity.ok(ApiResponse.ok(assessmentService.saveResultsBulk(id, schoolId, results, auth.getName(), roleOf(auth)))); }
    @GetMapping("/student/{studentId}")
    public ResponseEntity<ApiResponse<?>> getStudentResults(
            @PathVariable String schoolId, @PathVariable String studentId,
            @RequestParam(required = false) Integer page, @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sortBy, @RequestParam(required = false) String sortDir,
            Authentication auth) {
        boolean parent = "PARENT".equalsIgnoreCase(roleOf(auth));
        if (parent) assertParentOwnsStudent(schoolId, studentId, auth);
        if (parent) return ResponseEntity.ok(ApiResponse.ok(assessmentService.getStudentResults(schoolId, studentId, true, auth.getName(), roleOf(auth))));
        Pageable pageable = PageRequestUtil.build(page, size, sortBy, sortDir);
        if (pageable == null) return ResponseEntity.ok(ApiResponse.ok(assessmentService.getStudentResults(schoolId, studentId, false, auth.getName(), roleOf(auth))));
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.of(assessmentService.getStudentResultsPaged(schoolId, studentId, pageable, auth.getName(), roleOf(auth)))));
    }
    @GetMapping("/student/{studentId}/enriched") public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getEnrichedStudentResults(@PathVariable String schoolId, @PathVariable String studentId, Authentication auth) {
        boolean parent = "PARENT".equalsIgnoreCase(roleOf(auth));
        if (parent) assertParentOwnsStudent(schoolId, studentId, auth);
        return ResponseEntity.ok(ApiResponse.ok(assessmentService.getEnrichedStudentResults(schoolId, studentId, parent, auth.getName(), roleOf(auth))));
    }

    @PatchMapping("/{id}/submit")
    public ResponseEntity<ApiResponse<Assessment>> submit(@PathVariable String schoolId, @PathVariable String id, Authentication auth) {
        return ResponseEntity.ok(ApiResponse.ok(assessmentService.submitForVerification(id, schoolId, auth.getName(), roleOf(auth))));
    }

    @PatchMapping("/{id}/verify")
    public ResponseEntity<ApiResponse<Assessment>> verify(@PathVariable String schoolId, @PathVariable String id, Authentication auth) {
        return ResponseEntity.ok(ApiResponse.ok(assessmentService.verify(id, schoolId, auth.getName(), roleOf(auth))));
    }

    @PatchMapping("/{id}/reject")
    public ResponseEntity<ApiResponse<Assessment>> reject(@PathVariable String schoolId, @PathVariable String id,
                                                           @RequestBody Map<String, String> body, Authentication auth) {
        return ResponseEntity.ok(ApiResponse.ok(assessmentService.reject(id, schoolId, body.get("note"), auth.getName(), roleOf(auth))));
    }

    @PatchMapping("/{id}/publish-cycle")
    public ResponseEntity<ApiResponse<Map<String, Object>>> publishCycle(@PathVariable String schoolId, @PathVariable String id,
                                                                         Authentication auth) {
        return ResponseEntity.ok(ApiResponse.ok(assessmentService.publishCycle(id, schoolId, auth.getName(), roleOf(auth))));
    }

    private void assertParentOwnsStudent(String schoolId, String studentId, Authentication auth) {
        AppUser user = userRepository.findById(auth.getName())
                .orElseThrow(() -> new ForbiddenException("Authenticated parent was not found"));
        Student student = studentRepository.findByIdAndSchoolId(studentId, schoolId)
                .orElseThrow(() -> new ForbiddenException("Learner is not available to this parent"));
        boolean emailMatch = user.getEmail() != null && student.getGuardianEmail() != null
                && user.getEmail().equalsIgnoreCase(student.getGuardianEmail());
        boolean phoneMatch = user.getPhone() != null && student.getGuardianPhone() != null
                && normalizePhone(user.getPhone()).equalsIgnoreCase(normalizePhone(student.getGuardianPhone()));
        if (!emailMatch && !phoneMatch) {
            throw new ForbiddenException("Parents can only view results for their own children");
        }
    }

    private static String normalizePhone(String phone) {
        return phone.replaceAll("[\\s-]", "");
    }
}
