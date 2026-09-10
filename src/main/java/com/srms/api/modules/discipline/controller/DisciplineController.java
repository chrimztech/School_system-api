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
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController @RequestMapping("/api/schools/{schoolId}/discipline") @RequiredArgsConstructor
public class DisciplineController {
    private final DisciplineService disciplineService;
    private final UserRepository userRepository;
    private final StudentRepository studentRepository;

    private static String roleOf(Authentication auth) {
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst().map(a -> a.replaceFirst("^ROLE_", "")).orElse("");
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<DisciplineCase>>> getAll(@PathVariable String schoolId, Authentication auth) {
        if ("PARENT".equals(roleOf(auth))) {
            throw new ForbiddenException("Parents can only view their own children's discipline records");
        }
        return ResponseEntity.ok(ApiResponse.ok(disciplineService.findAll(schoolId)));
    }
    @GetMapping("/student/{studentId}")
    public ResponseEntity<ApiResponse<List<DisciplineCase>>> getByStudent(@PathVariable String schoolId, @PathVariable String studentId, Authentication auth) {
        if ("PARENT".equals(roleOf(auth))) assertParentOwnsStudent(schoolId, studentId, auth);
        return ResponseEntity.ok(ApiResponse.ok(disciplineService.findByStudent(schoolId, studentId)));
    }
    @PostMapping public ResponseEntity<ApiResponse<DisciplineCase>> create(@PathVariable String schoolId, @RequestBody DisciplineCaseDto dto) { return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(disciplineService.create(schoolId, dto))); }
    @PatchMapping("/{id}/resolve") public ResponseEntity<ApiResponse<DisciplineCase>> resolve(@PathVariable String schoolId, @PathVariable String id) { return ResponseEntity.ok(ApiResponse.ok(disciplineService.resolve(schoolId, id))); }

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
            throw new ForbiddenException("Parents can only view discipline records for their own children");
        }
    }
}
