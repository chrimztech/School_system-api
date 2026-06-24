package com.srms.api.modules.discipline.controller;
import com.srms.api.common.ApiResponse;
import com.srms.api.modules.discipline.dto.DisciplineCaseDto;
import com.srms.api.modules.discipline.entity.DisciplineCase;
import com.srms.api.modules.discipline.service.DisciplineService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController @RequestMapping("/api/schools/{schoolId}/discipline") @RequiredArgsConstructor
public class DisciplineController {
    private final DisciplineService disciplineService;
    @GetMapping public ResponseEntity<ApiResponse<List<DisciplineCase>>> getAll(@PathVariable String schoolId) { return ResponseEntity.ok(ApiResponse.ok(disciplineService.findAll(schoolId))); }
    @GetMapping("/student/{studentId}") public ResponseEntity<ApiResponse<List<DisciplineCase>>> getByStudent(@PathVariable String schoolId, @PathVariable String studentId) { return ResponseEntity.ok(ApiResponse.ok(disciplineService.findByStudent(schoolId, studentId))); }
    @PostMapping public ResponseEntity<ApiResponse<DisciplineCase>> create(@PathVariable String schoolId, @RequestBody DisciplineCaseDto dto) { return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(disciplineService.create(schoolId, dto))); }
    @PatchMapping("/{id}/resolve") public ResponseEntity<ApiResponse<DisciplineCase>> resolve(@PathVariable String schoolId, @PathVariable String id) { return ResponseEntity.ok(ApiResponse.ok(disciplineService.resolve(schoolId, id))); }
}
