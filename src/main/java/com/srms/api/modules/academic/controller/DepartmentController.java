package com.srms.api.modules.academic.controller;

import com.srms.api.common.ApiResponse;
import com.srms.api.modules.academic.entity.Department;
import com.srms.api.modules.academic.service.AcademicService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/schools/{schoolId}/departments")
@RequiredArgsConstructor
public class DepartmentController {
    private final AcademicService academicService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Department>>> getAll(@PathVariable String schoolId) {
        return ResponseEntity.ok(ApiResponse.ok(academicService.findAllDepartments(schoolId)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Department>> create(@PathVariable String schoolId, @RequestBody Department dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.created(academicService.createDepartment(schoolId, dto)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Department>> update(@PathVariable String schoolId, @PathVariable String id, @RequestBody Department dto) {
        return ResponseEntity.ok(ApiResponse.ok(academicService.updateDepartment(id, schoolId, dto)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String schoolId, @PathVariable String id) {
        academicService.deleteDepartment(id, schoolId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
