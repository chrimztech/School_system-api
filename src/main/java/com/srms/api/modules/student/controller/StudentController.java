package com.srms.api.modules.student.controller;

import com.srms.api.common.ApiResponse;
import com.srms.api.common.BulkImportResult;
import com.srms.api.common.PageRequestUtil;
import com.srms.api.common.PageResponse;
import com.srms.api.modules.academic.service.AcademicService;
import com.srms.api.modules.student.dto.StudentDto;
import com.srms.api.modules.student.entity.Student;
import com.srms.api.modules.student.service.StudentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/schools/{schoolId}/students")
@RequiredArgsConstructor
public class StudentController {
    private final StudentService studentService;
    private final AcademicService academicService;

    @GetMapping
    public ResponseEntity<ApiResponse<?>> getAll(
            @PathVariable String schoolId,
            @RequestParam(required = false) String teacherEmail,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDir) {
        if (teacherEmail != null && !teacherEmail.isBlank()) {
            return ResponseEntity.ok(ApiResponse.ok(academicService.findStudentsByTeacherEmail(schoolId, teacherEmail)));
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
            @RequestParam String email) {
        return ResponseEntity.ok(ApiResponse.ok(studentService.findByGuardianEmail(schoolId, email)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Student>> getById(@PathVariable String schoolId, @PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.ok(studentService.findById(id, schoolId)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Student>> create(@PathVariable String schoolId, @RequestBody StudentDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(studentService.create(schoolId, dto)));
    }

    @PostMapping("/bulk")
    public ResponseEntity<ApiResponse<BulkImportResult>> bulkCreate(@PathVariable String schoolId, @RequestBody List<StudentDto> dtos) {
        return ResponseEntity.ok(ApiResponse.ok(studentService.bulkCreate(schoolId, dtos)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Student>> update(@PathVariable String schoolId, @PathVariable String id, @RequestBody StudentDto dto) {
        return ResponseEntity.ok(ApiResponse.ok(studentService.update(id, schoolId, dto)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String schoolId, @PathVariable String id) {
        studentService.delete(id, schoolId);
        return ResponseEntity.ok(ApiResponse.ok("Student deactivated", null));
    }
}
