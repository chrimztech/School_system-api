package com.srms.api.modules.teacher.controller;
import com.srms.api.common.ApiResponse;
import com.srms.api.common.BulkImportResult;
import com.srms.api.modules.teacher.dto.TeacherDto;
import com.srms.api.modules.teacher.entity.Teacher;
import com.srms.api.modules.teacher.service.TeacherService;
import com.srms.api.security.RoleGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController @RequestMapping("/api/schools/{schoolId}/teachers") @RequiredArgsConstructor
public class TeacherController {
    private final TeacherService teacherService;
    @GetMapping public ResponseEntity<ApiResponse<List<Teacher>>> getAll(@PathVariable String schoolId) { return ResponseEntity.ok(ApiResponse.ok(teacherService.findAll(schoolId))); }
    @GetMapping("/{id}") public ResponseEntity<ApiResponse<Teacher>> getById(@PathVariable String schoolId, @PathVariable String id) { return ResponseEntity.ok(ApiResponse.ok(teacherService.findById(id, schoolId))); }
    @PostMapping public ResponseEntity<ApiResponse<Teacher>> create(@PathVariable String schoolId, @RequestBody TeacherDto dto) { return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(teacherService.create(schoolId, dto))); }
    @PostMapping("/bulk") public ResponseEntity<ApiResponse<BulkImportResult>> bulkCreate(@PathVariable String schoolId, @RequestBody List<TeacherDto> dtos) { return ResponseEntity.ok(ApiResponse.ok(teacherService.bulkCreate(schoolId, dtos))); }
    @PutMapping("/{id}") public ResponseEntity<ApiResponse<Teacher>> update(@PathVariable String schoolId, @PathVariable String id, @RequestBody TeacherDto dto) { return ResponseEntity.ok(ApiResponse.ok(teacherService.update(id, schoolId, dto))); }
    @DeleteMapping("/{id}") public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String schoolId, @PathVariable String id) { teacherService.delete(id, schoolId); return ResponseEntity.ok(ApiResponse.ok("Teacher deactivated", null)); }

    // Irreversible: erases the staff record and their own tied data (signature, teaching
    // assignments, timetable slots) — see TeacherService.deletePermanently's javadoc for
    // exactly what is and isn't touched. Restricted to school-account-manager roles.
    @DeleteMapping("/{id}/permanent")
    public ResponseEntity<ApiResponse<Void>> deletePermanently(@PathVariable String schoolId, @PathVariable String id, Authentication auth) {
        RoleGuard.requireSchoolAccountManager(auth);
        teacherService.deletePermanently(id, schoolId);
        return ResponseEntity.ok(ApiResponse.ok("Teacher permanently deleted", null));
    }
}
