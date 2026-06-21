package com.srms.api.modules.student.controller;

import com.srms.api.common.ApiResponse;
import com.srms.api.modules.student.dto.StudentDto;
import com.srms.api.modules.student.entity.Student;
import com.srms.api.modules.student.service.StudentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/schools/{schoolId}/students")
@RequiredArgsConstructor
public class StudentController {
    private final StudentService studentService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Student>>> getAll(@PathVariable String schoolId) {
        return ResponseEntity.ok(ApiResponse.ok(studentService.findAll(schoolId)));
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
