package com.srms.api.modules.health.controller;

import com.srms.api.common.ApiResponse;
import com.srms.api.common.PhoneUtils;
import com.srms.api.exception.ForbiddenException;
import com.srms.api.modules.auth.entity.AppUser;
import com.srms.api.modules.auth.repository.UserRepository;
import com.srms.api.modules.health.entity.HealthRecord;
import com.srms.api.modules.health.entity.HealthVisit;
import com.srms.api.modules.health.service.HealthService;
import com.srms.api.modules.student.entity.Student;
import com.srms.api.modules.student.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Medical records/visits are the most sensitive PII this app holds — every endpoint here must
 * either be staff-only or ownership-checked for a parent, never left open the way these were
 * before (any authenticated user of the school could pull every pupil's conditions, allergies,
 * and blood group with no check at all). */
@RestController @RequestMapping("/api/schools/{schoolId}/health") @RequiredArgsConstructor
public class HealthController {
    private final HealthService healthService;
    private final UserRepository userRepository;
    private final StudentRepository studentRepository;

    private static String roleOf(Authentication auth) {
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst().map(a -> a.replaceFirst("^ROLE_", "")).orElse("");
    }

    @GetMapping("/records")
    public ResponseEntity<ApiResponse<List<HealthRecord>>> getRecords(@PathVariable String schoolId, Authentication auth) {
        requireStaff(auth);
        return ResponseEntity.ok(ApiResponse.ok(healthService.getAllRecords(schoolId)));
    }
    @PostMapping("/records") public ResponseEntity<ApiResponse<HealthRecord>> saveRecord(@PathVariable String schoolId, @RequestBody HealthRecord record, Authentication auth) {
        requireStaff(auth);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(healthService.saveRecord(schoolId, record)));
    }
    @GetMapping("/records/{id}")
    public ResponseEntity<ApiResponse<HealthRecord>> getRecord(@PathVariable String schoolId, @PathVariable String id, Authentication auth) {
        HealthRecord record = healthService.getRecord(schoolId, id);
        if ("PARENT".equals(roleOf(auth))) assertParentOwnsStudent(schoolId, record.getStudentId(), auth);
        else requireStaff(auth);
        return ResponseEntity.ok(ApiResponse.ok(record));
    }
    @PutMapping("/records/{id}") public ResponseEntity<ApiResponse<HealthRecord>> updateRecord(@PathVariable String schoolId, @PathVariable String id, @RequestBody HealthRecord record, Authentication auth) {
        requireStaff(auth);
        record.setId(id);
        return ResponseEntity.ok(ApiResponse.ok(healthService.saveRecord(schoolId, record)));
    }
    @GetMapping("/records/student/{studentId}")
    public ResponseEntity<ApiResponse<HealthRecord>> getByStudent(@PathVariable String schoolId, @PathVariable String studentId, Authentication auth) {
        if ("PARENT".equals(roleOf(auth))) assertParentOwnsStudent(schoolId, studentId, auth);
        else requireStaff(auth);
        return ResponseEntity.ok(ApiResponse.ok(healthService.getRecordByStudent(schoolId, studentId)));
    }

    @GetMapping("/visits")
    public ResponseEntity<ApiResponse<List<HealthVisit>>> getVisits(@PathVariable String schoolId, Authentication auth) {
        requireStaff(auth);
        return ResponseEntity.ok(ApiResponse.ok(healthService.getAllVisits(schoolId)));
    }
    @PostMapping("/visits") public ResponseEntity<ApiResponse<HealthVisit>> createVisit(@PathVariable String schoolId, @RequestBody HealthVisit visit, Authentication auth) {
        requireStaff(auth);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(healthService.createVisit(schoolId, visit)));
    }

    private void requireStaff(Authentication auth) {
        if ("PARENT".equals(roleOf(auth))) {
            throw new ForbiddenException("Parents can only view their own children's health records");
        }
    }

    /** Same ownership rule as AttendanceController/AssessmentController/StudentController's
     * identically-named check. */
    private void assertParentOwnsStudent(String schoolId, String studentId, Authentication auth) {
        AppUser user = userRepository.findById(auth.getName())
                .orElseThrow(() -> new ForbiddenException("Authenticated parent was not found"));
        Student student = studentRepository.findByIdAndSchoolId(studentId, schoolId)
                .orElseThrow(() -> new ForbiddenException("Learner is not available to this parent"));
        boolean emailMatch = user.getEmail() != null && student.getGuardianEmail() != null
                && user.getEmail().equalsIgnoreCase(student.getGuardianEmail());
        boolean phoneMatch = user.getPhone() != null && student.getGuardianPhone() != null
                && PhoneUtils.normalize(user.getPhone()).equalsIgnoreCase(PhoneUtils.normalize(student.getGuardianPhone()));
        if (!emailMatch && !phoneMatch) {
            throw new ForbiddenException("Parents can only view health records for their own children");
        }
    }
}
