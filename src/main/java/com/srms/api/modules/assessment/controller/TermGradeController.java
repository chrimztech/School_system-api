package com.srms.api.modules.assessment.controller;

import com.srms.api.common.ApiResponse;
import com.srms.api.common.PhoneUtils;
import com.srms.api.exception.ForbiddenException;
import com.srms.api.modules.assessment.entity.Assessment;
import com.srms.api.modules.assessment.entity.PublishedTermGrade;
import com.srms.api.modules.assessment.entity.TermGrade;
import com.srms.api.modules.assessment.service.TermGradeService;
import com.srms.api.modules.auth.entity.AppUser;
import com.srms.api.modules.auth.repository.UserRepository;
import com.srms.api.modules.student.entity.Student;
import com.srms.api.modules.student.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/schools/{schoolId}/term-grades")
@RequiredArgsConstructor
public class TermGradeController {
    private final TermGradeService termGradeService;
    private final UserRepository userRepository;
    private final StudentRepository studentRepository;

    private static String roleOf(Authentication auth) {
        return auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).findFirst()
                .map(authority -> authority.replaceFirst("^ROLE_", "")).orElse("");
    }

    @PostMapping("/compute")
    public ResponseEntity<ApiResponse<List<TermGrade>>> compute(@PathVariable String schoolId,
                                                                 @RequestBody Map<String, String> body,
                                                                 Authentication auth) {
        assertActorSchool(schoolId, auth);
        if ("PARENT".equalsIgnoreCase(roleOf(auth))) {
            throw new ForbiddenException("Parents can only view published report cards");
        }
        List<TermGrade> result = termGradeService.compute(schoolId, body.get("classId"), body.get("subjectName"),
                body.get("term"), body.get("academicYear"));
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<TermGrade>>> history(
            @PathVariable String schoolId, @RequestParam String studentId,
            @RequestParam String academicYear, Authentication auth) {
        assertActorSchool(schoolId, auth);
        if ("PARENT".equalsIgnoreCase(roleOf(auth))) {
            throw new ForbiddenException("Parents can only view published report cards");
        }
        return ResponseEntity.ok(ApiResponse.ok(
                termGradeService.getHistory(schoolId, studentId, academicYear, true)));
    }

    @GetMapping("/published")
    public ResponseEntity<ApiResponse<List<PublishedTermGrade>>> publishedHistory(
            @PathVariable String schoolId, @RequestParam String studentId,
            @RequestParam(required = false) String academicYear,
            @RequestParam(required = false) Assessment.ReportingPeriod reportingPeriod,
            Authentication auth) {
        assertActorSchool(schoolId, auth);
        assertParentOwnsStudent(schoolId, studentId, auth);
        return ResponseEntity.ok(ApiResponse.ok(
                termGradeService.getPublishedHistory(schoolId, studentId, academicYear, reportingPeriod)));
    }

    @GetMapping("/class-stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> classStats(
            @PathVariable String schoolId, @RequestParam String classId,
            @RequestParam String subjectName, @RequestParam String term,
            @RequestParam String academicYear, @RequestParam Assessment.ReportingPeriod reportingPeriod,
            Authentication auth) {
        assertActorSchool(schoolId, auth);
        return ResponseEntity.ok(ApiResponse.ok(termGradeService.getPublishedClassStats(
                schoolId, classId, subjectName, term, academicYear, reportingPeriod)));
    }

    private void assertActorSchool(String schoolId, Authentication auth) {
        if ("SUPER_ADMIN".equalsIgnoreCase(roleOf(auth))) return;
        String actorSchool = auth.getCredentials() == null ? null : auth.getCredentials().toString();
        if (!schoolId.equals(actorSchool)) {
            throw new ForbiddenException("You cannot access another school's report cards");
        }
    }

    private void assertParentOwnsStudent(String schoolId, String studentId, Authentication auth) {
        if (!"PARENT".equalsIgnoreCase(roleOf(auth))) return;
        AppUser user = userRepository.findById(auth.getName())
                .orElseThrow(() -> new ForbiddenException("Authenticated parent was not found"));
        Student student = studentRepository.findByIdAndSchoolId(studentId, schoolId)
                .orElseThrow(() -> new ForbiddenException("Learner is not available to this parent"));
        boolean emailMatch = user.getEmail() != null && student.getGuardianEmail() != null
                && user.getEmail().equalsIgnoreCase(student.getGuardianEmail());
        boolean phoneMatch = user.getPhone() != null && student.getGuardianPhone() != null
                && normalizePhone(user.getPhone()).equalsIgnoreCase(normalizePhone(student.getGuardianPhone()));
        if (!emailMatch && !phoneMatch) {
            throw new ForbiddenException("Parents can only view report cards for their own children");
        }
    }

    private static String normalizePhone(String phone) {
        return PhoneUtils.normalize(phone);
    }
}
