package com.srms.api.modules.timetable.controller;

import com.srms.api.common.ApiResponse;
import com.srms.api.modules.timetable.entity.TimetableSlot;
import com.srms.api.modules.timetable.service.TimetableService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @RequestMapping("/api/schools/{schoolId}/timetable") @RequiredArgsConstructor
public class TimetableController {
    private final TimetableService timetableService;
    @GetMapping public ResponseEntity<ApiResponse<List<TimetableSlot>>> getAll(@PathVariable String schoolId, @RequestParam(required = false) String classId, @RequestParam(required = false) String teacherId) {
        if (classId != null) return ResponseEntity.ok(ApiResponse.ok(timetableService.getByClass(schoolId, classId)));
        if (teacherId != null) return ResponseEntity.ok(ApiResponse.ok(timetableService.getByTeacher(schoolId, teacherId)));
        return ResponseEntity.ok(ApiResponse.ok(timetableService.getAll(schoolId)));
    }
    @PostMapping public ResponseEntity<ApiResponse<TimetableSlot>> create(@PathVariable String schoolId, @RequestBody TimetableSlot slot) { return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(timetableService.create(schoolId, slot))); }
    @PutMapping("/{id}") public ResponseEntity<ApiResponse<TimetableSlot>> update(@PathVariable String schoolId, @PathVariable String id, @RequestBody TimetableSlot slot) { return ResponseEntity.ok(ApiResponse.ok(timetableService.update(schoolId, id, slot))); }
    @DeleteMapping("/{id}") public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String schoolId, @PathVariable String id) { timetableService.delete(schoolId, id); return ResponseEntity.ok(ApiResponse.ok("Deleted", null)); }
}
