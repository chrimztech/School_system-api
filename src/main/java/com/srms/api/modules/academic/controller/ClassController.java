package com.srms.api.modules.academic.controller;

import com.srms.api.common.ApiResponse;
import com.srms.api.modules.academic.entity.*;
import com.srms.api.modules.academic.service.AcademicService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/schools/{schoolId}/classes")
@RequiredArgsConstructor
public class ClassController {
    private final AcademicService academicService;

    // ── Classes ──────────────────────────────────────────────────
    @GetMapping
    public ResponseEntity<ApiResponse<List<SchoolClass>>> getAll(
            @PathVariable String schoolId,
            @RequestParam(required = false) String teacherEmail) {
        List<SchoolClass> result = (teacherEmail != null && !teacherEmail.isBlank())
            ? academicService.findClassesByTeacherEmail(schoolId, teacherEmail)
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
