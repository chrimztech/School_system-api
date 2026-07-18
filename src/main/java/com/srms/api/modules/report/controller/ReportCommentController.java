package com.srms.api.modules.report.controller;

import com.srms.api.common.ApiResponse;
import com.srms.api.exception.ForbiddenException;
import com.srms.api.modules.report.entity.ReportComment;
import com.srms.api.modules.report.service.ReportCommentService;
import com.srms.api.modules.auth.entity.AppUser;
import com.srms.api.modules.auth.repository.UserRepository;
import com.srms.api.modules.student.entity.Student;
import com.srms.api.modules.student.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/schools/{schoolId}/report-comments")
@RequiredArgsConstructor
public class ReportCommentController {
    private final ReportCommentService service;
    private final UserRepository userRepository;
    private final StudentRepository studentRepository;

    /** Anyone who can edit either the teacher or head comment on the report-card page. */
    private static final Set<String> CAN_WRITE_ROLES = Set.of(
            "SUPER_ADMIN", "SCHOOL_ADMIN", "TEACHER", "PRINCIPAL", "DEPUTY_HEAD", "HOD");

    @GetMapping("/student/{studentId}")
    public ResponseEntity<ApiResponse<ReportComment>> get(
            @PathVariable String schoolId,
            @PathVariable String studentId,
            @RequestParam String term,
            @RequestParam String academicYear,
            Authentication auth) {
        assertSchoolAndParentAccess(schoolId, studentId, auth);
        return service.find(schoolId, studentId, term, academicYear)
                .map(rc -> ResponseEntity.ok(ApiResponse.ok(rc)))
                .orElse(ResponseEntity.ok(ApiResponse.ok(null)));
    }

    @PutMapping("/student/{studentId}")
    public ResponseEntity<ApiResponse<ReportComment>> upsert(
            @PathVariable String schoolId,
            @PathVariable String studentId,
            @RequestParam String term,
            @RequestParam String academicYear,
            @RequestBody Map<String, String> body,
            Authentication auth) {
        assertSchoolAndParentAccess(schoolId, studentId, auth);
        String role = roleOf(auth);
        if (!CAN_WRITE_ROLES.contains(role)) {
            throw new ForbiddenException("Your role does not have permission to edit report card comments");
        }
        ReportComment rc = service.upsert(schoolId, studentId, term, academicYear,
                body.get("teacherComment"), body.get("headComment"));
        return ResponseEntity.ok(ApiResponse.ok(rc));
    }

    private void assertSchoolAndParentAccess(String schoolId, String studentId, Authentication auth) {
        String role = roleOf(auth);
        if (!"SUPER_ADMIN".equals(role)) {
            String actorSchool = auth.getCredentials() == null ? null : auth.getCredentials().toString();
            if (!schoolId.equals(actorSchool)) {
                throw new ForbiddenException("You cannot access another school's report cards");
            }
        }
        if (!"PARENT".equals(role)) return;
        AppUser parent = userRepository.findById(auth.getName())
                .orElseThrow(() -> new ForbiddenException("Authenticated parent was not found"));
        Student student = studentRepository.findByIdAndSchoolId(studentId, schoolId)
                .orElseThrow(() -> new ForbiddenException("Learner is not available to this parent"));
        if (parent.getEmail() == null || student.getGuardianEmail() == null
                || !parent.getEmail().equalsIgnoreCase(student.getGuardianEmail())) {
            throw new ForbiddenException("Parents can only view report cards for their own children");
        }
    }

    private static String roleOf(Authentication auth) {
        return auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).findFirst()
                .map(authority -> authority.replaceFirst("^ROLE_", "")).orElse("");
    }
}
