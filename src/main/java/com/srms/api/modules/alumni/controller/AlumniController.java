package com.srms.api.modules.alumni.controller;

import com.srms.api.common.ApiResponse;
import com.srms.api.modules.alumni.entity.AlumniRecord;
import com.srms.api.modules.alumni.service.AlumniService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @RequestMapping("/api/schools/{schoolId}/alumni") @RequiredArgsConstructor
public class AlumniController {
    private final AlumniService alumniService;
    @GetMapping public ResponseEntity<ApiResponse<List<AlumniRecord>>> getAll(@PathVariable String schoolId) { return ResponseEntity.ok(ApiResponse.ok(alumniService.getAll(schoolId))); }
    @PostMapping public ResponseEntity<ApiResponse<AlumniRecord>> create(@PathVariable String schoolId, @RequestBody AlumniRecord record) { return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(alumniService.create(schoolId, record))); }
    @GetMapping("/{id}") public ResponseEntity<ApiResponse<AlumniRecord>> getOne(@PathVariable String schoolId, @PathVariable String id) { return ResponseEntity.ok(ApiResponse.ok(alumniService.getOne(schoolId, id))); }
    @PutMapping("/{id}") public ResponseEntity<ApiResponse<AlumniRecord>> update(@PathVariable String schoolId, @PathVariable String id, @RequestBody AlumniRecord record) { return ResponseEntity.ok(ApiResponse.ok(alumniService.update(schoolId, id, record))); }
    @DeleteMapping("/{id}") public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String schoolId, @PathVariable String id) { alumniService.delete(schoolId, id); return ResponseEntity.ok(ApiResponse.ok("Deleted", null)); }
}
