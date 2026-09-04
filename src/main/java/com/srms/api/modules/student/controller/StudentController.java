package com.srms.api.modules.student.controller;

import com.srms.api.common.ApiResponse;
import com.srms.api.common.BulkImportResult;
import com.srms.api.common.PageRequestUtil;
import com.srms.api.common.PageResponse;
import com.srms.api.common.PhoneUtils;
import com.srms.api.exception.ForbiddenException;
import com.srms.api.modules.academic.service.AcademicService;
import com.srms.api.modules.auth.entity.AppUser;
import com.srms.api.modules.auth.repository.UserRepository;
import com.srms.api.modules.student.dto.StudentDto;
import com.srms.api.modules.student.entity.Student;
import com.srms.api.modules.student.service.StudentService;
import com.srms.api.security.ModuleAccessService;
import com.srms.api.security.RoleGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/schools/{schoolId}/students")
@RequiredArgsConstructor
public class StudentController {
    private final StudentService studentService;
    private final AcademicService academicService;
    private final UserRepository userRepository;
    private final ModuleAccessService moduleAccessService;

    // Mirrors RoleGuard's own (private) SCHOOL_ACCOUNT_MANAGERS set.
    private static final Set<String> SCHOOL_ACCOUNT_MANAGERS = Set.of(
            "SUPER_ADMIN", "SCHOOL_ADMIN", "PRINCIPAL", "DEPUTY_HEAD");

    private static String roleOf(Authentication auth) {
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst().map(a -> a.replaceFirst("^ROLE_", "")).orElse("");
    }

    private void requireManage(String schoolId, Authentication auth) {
        if (!moduleAccessService.isAllowed(schoolId, auth, "students", "full", SCHOOL_ACCOUNT_MANAGERS.contains(RoleGuard.roleOf(auth)))) {
            throw new ForbiddenException("Your role cannot manage school accounts or permissions");
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<?>> getAll(
            @PathVariable String schoolId,
            @RequestParam(required = false) String teacherEmail,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDir,
            Authentication auth) {
        // A teacher's scoping is derived from their own authenticated identity, never trusted
        // from the client — otherwise any teacher could see the full school roster simply by
        // omitting (or forging) the teacherEmail query param on a direct API call.
        String effectiveTeacherEmail = teacherEmail;
        if ("TEACHER".equals(roleOf(auth))) {
            effectiveTeacherEmail = userRepository.findById(auth.getName())
                    .map(AppUser::getEmail)
                    .orElse(teacherEmail);
        }
        if (effectiveTeacherEmail != null && !effectiveTeacherEmail.isBlank()) {
            return ResponseEntity.ok(ApiResponse.ok(academicService.findStudentsByTeacherEmail(schoolId, effectiveTeacherEmail)));
        }
        Pageable pageable = PageRequestUtil.build(page, size, sortBy, sortDir);
        if (pageable == null) {
            return ResponseEntity.ok(ApiResponse.ok(studentService.findAll(schoolId)));
        }
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.of(studentService.findAllPaged(schoolId, pageable))));
    }

    @GetMapping("/by-guardian")
    public ResponseEntity<ApiResponse<List<Student>>> getByGuardian(
            @PathVariable String schoolId,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String phone,
            Authentication auth) {
        // Every current caller (Parent Portal, My Children, fees, report cards) only ever looks
        // up its own logged-in user — there's no legitimate case for looking up another
        // guardian's children by their email/phone, so the authenticated identity always wins
        // over whatever the client sent, closing off that lookup as an enumeration vector.
        AppUser self = userRepository.findById(auth.getName()).orElse(null);
        String effectiveEmail = self != null ? self.getEmail() : email;
        String effectivePhone = self != null ? self.getPhone() : phone;
        List<Student> students = (effectiveEmail != null && !effectiveEmail.isBlank())
                ? studentService.findByGuardianEmail(schoolId, effectiveEmail)
                : studentService.findByGuardianPhone(schoolId, effectivePhone);
        return ResponseEntity.ok(ApiResponse.ok(students));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Student>> getById(@PathVariable String schoolId, @PathVariable String id, Authentication auth) {
        Student student = studentService.findById(id, schoolId);
        if ("PARENT".equals(roleOf(auth))) assertParentOwnsStudent(student, auth);
        return ResponseEntity.ok(ApiResponse.ok(student));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Student>> create(@PathVariable String schoolId, @RequestBody StudentDto dto, Authentication auth) {
        requireManage(schoolId, auth);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(studentService.create(schoolId, dto)));
    }

    @PostMapping("/bulk")
    public ResponseEntity<ApiResponse<BulkImportResult>> bulkCreate(@PathVariable String schoolId, @RequestBody List<StudentDto> dtos, Authentication auth) {
        requireManage(schoolId, auth);
        return ResponseEntity.ok(ApiResponse.ok(studentService.bulkCreate(schoolId, dtos)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Student>> update(@PathVariable String schoolId, @PathVariable String id, @RequestBody StudentDto dto, Authentication auth) {
        requireManage(schoolId, auth);
        return ResponseEntity.ok(ApiResponse.ok(studentService.update(id, schoolId, dto)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String schoolId, @PathVariable String id, Authentication auth) {
        requireManage(schoolId, auth);
        studentService.delete(id, schoolId);
        return ResponseEntity.ok(ApiResponse.ok("Student deactivated", null));
    }

    /** Same ownership rule as AssessmentController's identically-named check — a parent may only
     * reach a student record that matches their own authenticated email/phone as guardian. */
    private void assertParentOwnsStudent(Student student, Authentication auth) {
        AppUser user = userRepository.findById(auth.getName())
                .orElseThrow(() -> new ForbiddenException("Authenticated parent was not found"));
        boolean emailMatch = user.getEmail() != null && student.getGuardianEmail() != null
                && user.getEmail().equalsIgnoreCase(student.getGuardianEmail());
        boolean phoneMatch = user.getPhone() != null && student.getGuardianPhone() != null
                && PhoneUtils.normalize(user.getPhone()).equalsIgnoreCase(PhoneUtils.normalize(student.getGuardianPhone()));
        if (!emailMatch && !phoneMatch) {
            throw new ForbiddenException("Parents can only view their own children");
        }
    }
}
