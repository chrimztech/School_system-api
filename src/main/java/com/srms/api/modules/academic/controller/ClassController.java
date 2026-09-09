package com.srms.api.modules.academic.controller;

import com.srms.api.common.ApiResponse;
import com.srms.api.exception.ForbiddenException;
import com.srms.api.modules.academic.dto.GradeOffsetFixResult;
import com.srms.api.modules.academic.entity.*;
import com.srms.api.modules.academic.service.AcademicService;
import com.srms.api.modules.auth.entity.AppUser;
import com.srms.api.modules.auth.repository.UserRepository;
import com.srms.api.security.RoleGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/schools/{schoolId}/classes")
@RequiredArgsConstructor
public class ClassController {
    private final AcademicService academicService;
    private final UserRepository userRepository;

    private static String roleOf(Authentication auth) {
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst().map(a -> a.replaceFirst("^ROLE_", "")).orElse("");
    }

    // ── Classes ──────────────────────────────────────────────────
    @GetMapping
    public ResponseEntity<ApiResponse<List<SchoolClass>>> getAll(
            @PathVariable String schoolId,
            @RequestParam(required = false) String teacherEmail,
            Authentication auth) {
        // Same reasoning as StudentController: a teacher's own identity is the only trustworthy
        // source for their scoping — a client-supplied teacherEmail (or its absence) must never
        // be able to widen what a TEACHER-role caller sees.
        String effectiveTeacherEmail = teacherEmail;
        if ("TEACHER".equals(roleOf(auth))) {
            effectiveTeacherEmail = userRepository.findById(auth.getName())
                    .map(AppUser::getEmail)
                    .orElse(teacherEmail);
        }
        List<SchoolClass> result = (effectiveTeacherEmail != null && !effectiveTeacherEmail.isBlank())
            ? academicService.findClassesByTeacherEmail(schoolId, effectiveTeacherEmail)
            : academicService.findAllClasses(schoolId);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SchoolClass>> getById(@PathVariable String schoolId, @PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.ok(academicService.findClassById(id, schoolId)));
    }
    @PostMapping
    public ResponseEntity<ApiResponse<SchoolClass>> create(@PathVariable String schoolId, @RequestBody SchoolClass dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(academicService.createClass(schoolId, dto)));
    }
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<SchoolClass>> update(@PathVariable String schoolId, @PathVariable String id, @RequestBody SchoolClass dto) {
        return ResponseEntity.ok(ApiResponse.ok(academicService.updateClass(id, schoolId, dto)));
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String schoolId, @PathVariable String id) {
        academicService.deleteClass(id, schoolId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    // One-time repair for the pre-fix classes.tsx bug that skipped the COMBINED/FULL Form
    // grade offset — see AcademicService.fixSecondaryGradeOffset's javadoc. School-account-
    // manager-only: it rewrites SchoolClass.grade and Student.grade directly.
    @PostMapping("/fix-secondary-grade-offset")
    public ResponseEntity<ApiResponse<GradeOffsetFixResult>> fixSecondaryGradeOffset(
            @PathVariable String schoolId, Authentication auth) {
        RoleGuard.requireSchoolAccountManager(auth);
        if (!RoleGuard.isSuperAdmin(auth)) {
            String actorSchool = auth.getCredentials() == null ? null : auth.getCredentials().toString();
            if (!schoolId.equals(actorSchool)) {
                throw new ForbiddenException("You cannot repair another school's data");
            }
        }
        return ResponseEntity.ok(ApiResponse.ok(academicService.fixSecondaryGradeOffset(schoolId)));
    }

    // ── Class enrolments (student → class) ───────────────────────
    @GetMapping("/{classId}/enrolments")
    public ResponseEntity<ApiResponse<List<ClassEnrolment>>> getEnrolments(@PathVariable String schoolId, @PathVariable String classId) {
        return ResponseEntity.ok(ApiResponse.ok(academicService.getClassEnrolments(classId, schoolId)));
    }
    @PostMapping("/{classId}/enrolments")
    public ResponseEntity<ApiResponse<ClassEnrolment>> enrol(@PathVariable String schoolId, @PathVariable String classId, @RequestBody ClassEnrolment dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(academicService.enrolStudent(classId, schoolId, dto)));
    }
    @DeleteMapping("/{classId}/enrolments/{enrolmentId}")
    public ResponseEntity<ApiResponse<Void>> removeEnrolment(@PathVariable String schoolId, @PathVariable String classId, @PathVariable String enrolmentId) {
        academicService.removeEnrolment(classId, enrolmentId, schoolId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    // ── Teacher-class-subject assignments ────────────────────────
    @GetMapping("/assignments")
    public ResponseEntity<ApiResponse<List<TeacherClassSubject>>> getAssignmentsByTeacherEmail(
            @PathVariable String schoolId,
            @RequestParam String teacherEmail,
            Authentication auth) {
        String effectiveTeacherEmail = teacherEmail;
        if ("TEACHER".equals(roleOf(auth))) {
            effectiveTeacherEmail = userRepository.findById(auth.getName())
                    .map(AppUser::getEmail)
                    .orElse(teacherEmail);
        }
        return ResponseEntity.ok(ApiResponse.ok(academicService.findAssignmentsByTeacherEmail(schoolId, effectiveTeacherEmail)));
    }
    @GetMapping("/{classId}/teachers")
    public ResponseEntity<ApiResponse<List<TeacherClassSubject>>> getTeachers(@PathVariable String schoolId, @PathVariable String classId) {
        return ResponseEntity.ok(ApiResponse.ok(academicService.getClassTeachers(classId, schoolId)));
    }
    @PostMapping("/{classId}/teachers")
    public ResponseEntity<ApiResponse<TeacherClassSubject>> assignTeacher(@PathVariable String schoolId, @PathVariable String classId, @RequestBody TeacherClassSubject dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(academicService.assignTeacher(classId, schoolId, dto)));
    }
    @DeleteMapping("/{classId}/teachers/{assignmentId}")
    public ResponseEntity<ApiResponse<Void>> removeTeacher(@PathVariable String schoolId, @PathVariable String classId, @PathVariable String assignmentId) {
        academicService.removeTeacherAssignment(classId, assignmentId, schoolId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
