package com.srms.api.modules.insights.controller;

import com.srms.api.common.ApiResponse;
import com.srms.api.common.PhoneUtils;
import com.srms.api.exception.ForbiddenException;
import com.srms.api.modules.auth.entity.AppUser;
import com.srms.api.modules.auth.repository.UserRepository;
import com.srms.api.modules.insights.dto.PerformanceInsightView;
import com.srms.api.modules.insights.service.PerformanceInsightService;
import com.srms.api.modules.student.entity.Student;
import com.srms.api.modules.student.repository.StudentRepository;
import com.srms.api.security.RoleGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Set;

/**
 * A pupil's weak subjects and nearby tuition centre matches. Readable by the pupil's own parent
 * and by school leadership (advising a family); deliberately not opened to a plain teacher here
 * — this is a parent-advisory view, not a mark-entry one, and teachers already see the raw
 * results elsewhere.
 */
@RestController
@RequestMapping("/api/schools/{schoolId}/students/{studentId}/performance-insight")
@RequiredArgsConstructor
public class PerformanceInsightController {
    private static final Set<String> LEADERSHIP_ROLES = Set.of(
            "SUPER_ADMIN", "SCHOOL_ADMIN", "PRINCIPAL", "DEPUTY_HEAD", "HOD");

    private final PerformanceInsightService service;
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<PerformanceInsightView>> get(
            @PathVariable String schoolId, @PathVariable String studentId,
            @RequestParam(required = false) String academicYear, Authentication auth) {
        assertCanView(schoolId, studentId, auth);
        return ResponseEntity.ok(ApiResponse.ok(service.analyze(schoolId, studentId, academicYear)));
    }

    @PostMapping("/narrative")
    public ResponseEntity<ApiResponse<Map<String, String>>> narrative(
            @PathVariable String schoolId, @PathVariable String studentId,
            @RequestParam(required = false) String academicYear, Authentication auth) {
        assertCanView(schoolId, studentId, auth);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("narrative", service.generateNarrative(schoolId, studentId, academicYear))));
    }

    private void assertCanView(String schoolId, String studentId, Authentication auth) {
        String role = RoleGuard.roleOf(auth);
        if (LEADERSHIP_ROLES.contains(role)) return;
        if (!"PARENT".equalsIgnoreCase(role)) {
            throw new ForbiddenException("Your role does not have access to performance insights");
        }
        AppUser user = userRepository.findById(auth.getName())
                .orElseThrow(() -> new ForbiddenException("Authenticated parent was not found"));
        Student student = studentRepository.findByIdAndSchoolId(studentId, schoolId)
                .orElseThrow(() -> new ForbiddenException("Learner is not available to this parent"));
        boolean emailMatch = user.getEmail() != null && student.getGuardianEmail() != null
                && user.getEmail().equalsIgnoreCase(student.getGuardianEmail());
        boolean phoneMatch = user.getPhone() != null && student.getGuardianPhone() != null
                && PhoneUtils.normalize(user.getPhone()).equalsIgnoreCase(PhoneUtils.normalize(student.getGuardianPhone()));
        if (!emailMatch && !phoneMatch) {
            throw new ForbiddenException("Parents can only view performance insights for their own children");
        }
    }
}
