package com.srms.api.modules.academic.controller;
import com.srms.api.common.ApiResponse;
import com.srms.api.modules.academic.entity.Subject;
import com.srms.api.modules.academic.service.AcademicService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController @RequestMapping("/api/schools/{schoolId}/subjects") @RequiredArgsConstructor
public class SubjectController {
    private final AcademicService academicService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Subject>>> getAll(@PathVariable String schoolId) {
        return ResponseEntity.ok(ApiResponse.ok(academicService.findAllSubjects(schoolId)));
    }
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Subject>> getById(@PathVariable String schoolId, @PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.ok(academicService.findSubjectById(id, schoolId)));
    }
    @PostMapping
    public ResponseEntity<ApiResponse<Subject>> create(@PathVariable String schoolId, @RequestBody Subject dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(academicService.createSubject(schoolId, dto)));
    }
    @PostMapping("/bulk")
    public ResponseEntity<ApiResponse<List<Subject>>> bulkCreate(@PathVariable String schoolId, @RequestBody List<Subject> subjects) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(academicService.bulkCreateSubjects(schoolId, subjects)));
    }
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Subject>> update(@PathVariable String schoolId, @PathVariable String id, @RequestBody Subject dto) {
        return ResponseEntity.ok(ApiResponse.ok(academicService.updateSubject(id, schoolId, dto)));
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String schoolId, @PathVariable String id) {
        academicService.deleteSubject(id, schoolId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
