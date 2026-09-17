package com.srms.api.modules.discipline.controller;
import com.srms.api.common.ApiResponse;
import com.srms.api.common.PhoneUtils;
import com.srms.api.exception.ForbiddenException;
import com.srms.api.modules.auth.entity.AppUser;
import com.srms.api.modules.auth.repository.UserRepository;
import com.srms.api.modules.discipline.dto.DisciplineCaseDto;
import com.srms.api.modules.discipline.entity.DisciplineCase;
import com.srms.api.modules.discipline.service.DisciplineService;
import com.srms.api.modules.student.entity.Student;
import com.srms.api.modules.student.repository.StudentRepository;
import com.srms.api.security.ModuleAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Set;

/** Role sets mirror the frontend's own ACCESS matrix (auth.tsx) for "discipline": TEACHER, HOD
 * and leadership get full read/write, CAREER_GUIDANCE is read-only, FINANCE and PARENT have
 * none — except a parent may still read their own child's cases specifically. */
@RestController @RequestMapping("/api/schools/{schoolId}/discipline") @RequiredArgsConstructor
public class DisciplineController {
    private final DisciplineService disciplineService;
    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final ModuleAccessService moduleAccessService;

    private static final Set<String> FULL_ROLES = Set.of(
            "SUPER_ADMIN", "SCHOOL_ADMIN", "PRINCIPAL", "DEPUTY_HEAD", "TEACHER", "HOD");
    private static final Set<String> READ_ROLES = Set.of(
            "SUPER_ADMIN", "SCHOOL_ADMIN", "PRINCIPAL", "DEPUTY_HEAD", "TEACHER", "HOD", "CAREER_GUIDANCE");

    private static String roleOf(Authentication auth) {
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst().map(a -> a.replaceFirst("^ROLE_", "")).orElse("");
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<DisciplineCase>>> getAll(@PathVariable String schoolId, Authentication auth) {
        requireRead(schoolId, auth);
        return ResponseEntity.ok(ApiResponse.ok(disciplineService.findAll(schoolId)));
    }
    @GetMapping("/student/{studentId}")
    public ResponseEntity<ApiResponse<List<DisciplineCase>>> getByStudent(@PathVariable String schoolId, @PathVariable String studentId, Authentication auth) {
        if ("PARENT".equals(roleOf(auth))) assertParentOwnsStudent(schoolId, studentId, auth);
        else requireRead(schoolId, auth);
        return ResponseEntity.ok(ApiResponse.ok(disciplineService.findByStudent(schoolId, studentId)));
    }
    @PostMapping public ResponseEntity<ApiResponse<DisciplineCase>> create(@PathVariable String schoolId, @RequestBody DisciplineCaseDto dto, Authentication auth) { requireFull(schoolId, auth); return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(disciplineService.create(schoolId, dto))); }
    @PatchMapping("/{id}/resolve") public ResponseEntity<ApiResponse<DisciplineCase>> resolve(@PathVariable String schoolId, @PathVariable String id, Authentication auth) { requireFull(schoolId, auth); return ResponseEntity.ok(ApiResponse.ok(disciplineService.resolve(schoolId, id))); }

    private void requireRead(String schoolId, Authentication auth) {
        if (!moduleAccessService.isAllowed(schoolId, auth, "discipline", "read", READ_ROLES.contains(roleOf(auth)))) {
            throw new ForbiddenException("Your role cannot access discipline records");
        }
    }

    private void requireFull(String schoolId, Authentication auth) {
        if (!moduleAccessService.isAllowed(schoolId, auth, "discipline", "full", FULL_ROLES.contains(roleOf(auth)))) {
            throw new ForbiddenException("Your role cannot record or resolve discipline cases");
        }
    }

    /** Same ownership rule as AttendanceController/AssessmentController/StudentController's
     * identically-named check. */
    private void assertParentOwnsStudent(String schoolId, String studentId, Authentication auth) {
        AppUser user = userRepository.findById(auth.getName())
                .orElseThrow(() -> new ForbiddenException("Authenticated parent was not found"));
        Student student = studentRepository.findByIdAndSchoolId(studentId, schoolId)
                .orElseThrow(() -> new ForbiddenException("Learner is not available to this parent"));
        // A captured guardian name is not required — the email/phone match below is enough on
        // its own to confirm ownership (product decision: must work even when the pupil's
        // guardian name was never typed in, as long as the contact is attached to the pupil).
        boolean emailMatch = user.getEmail() != null && student.getGuardianEmail() != null
                && user.getEmail().equalsIgnoreCase(student.getGuardianEmail());
        boolean phoneMatch = user.getPhone() != null && student.getGuardianPhone() != null
                && PhoneUtils.normalize(user.getPhone()).equalsIgnoreCase(PhoneUtils.normalize(student.getGuardianPhone()));
        if (!emailMatch && !phoneMatch) {
            throw new ForbiddenException("Parents can only view discipline records for their own children");
        }
    }
}
