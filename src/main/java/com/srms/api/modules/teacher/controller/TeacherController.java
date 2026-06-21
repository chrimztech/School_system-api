package com.srms.api.modules.teacher.controller;
import com.srms.api.common.ApiResponse;
import com.srms.api.modules.teacher.dto.TeacherDto;
import com.srms.api.modules.teacher.entity.Teacher;
import com.srms.api.modules.teacher.service.TeacherService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController @RequestMapping("/api/schools/{schoolId}/teachers") @RequiredArgsConstructor
public class TeacherController {
    private final TeacherService teacherService;
    @GetMapping public ResponseEntity<ApiResponse<List<Teacher>>> getAll(@PathVariable String schoolId) { return ResponseEntity.ok(ApiResponse.ok(teacherService.findAll(schoolId))); }
    @GetMapping("/{id}") public ResponseEntity<ApiResponse<Teacher>> getById(@PathVariable String schoolId, @PathVariable String id) { return ResponseEntity.ok(ApiResponse.ok(teacherService.findById(id, schoolId))); }
    @PostMapping public ResponseEntity<ApiResponse<Teacher>> create(@PathVariable String schoolId, @RequestBody TeacherDto dto) { return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(teacherService.create(schoolId, dto))); }
    @PutMapping("/{id}") public ResponseEntity<ApiResponse<Teacher>> update(@PathVariable String schoolId, @PathVariable String id, @RequestBody TeacherDto dto) { return ResponseEntity.ok(ApiResponse.ok(teacherService.update(id, schoolId, dto))); }
    @DeleteMapping("/{id}") public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String schoolId, @PathVariable String id) { teacherService.delete(id, schoolId); return ResponseEntity.ok(ApiResponse.ok("Teacher deactivated", null)); }
}
