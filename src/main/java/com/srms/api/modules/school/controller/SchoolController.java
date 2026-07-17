package com.srms.api.modules.school.controller;

import com.srms.api.common.ApiResponse;
import com.srms.api.modules.school.dto.SchoolDto;
import com.srms.api.modules.school.service.SchoolService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class SchoolController {
    private final SchoolService schoolService;

    // ── Public (no auth) ────────────────────────────────────────────────────
    @GetMapping("/api/public/schools/by-slug/{slug}")
    public ResponseEntity<?> getBySlug(@PathVariable String slug) {
        return schoolService.findPublicBySlug(slug)
                .<ResponseEntity<?>>map(summary -> ResponseEntity.ok(ApiResponse.ok(summary)))
                .orElseGet(() -> ResponseEntity.status(404).body(ApiResponse.error("School not found")));
    }

    // ── Authenticated ────────────────────────────────────────────────────────
    @GetMapping("/api/schools")
    public ResponseEntity<ApiResponse<List<SchoolDto>>> getAll() {
        return ResponseEntity.ok(ApiResponse.ok(schoolService.findAll()));
    }

    @GetMapping("/api/schools/{id}")
    public ResponseEntity<ApiResponse<SchoolDto>> getById(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.ok(schoolService.findById(id)));
    }

    @PostMapping("/api/schools")
    public ResponseEntity<ApiResponse<SchoolDto>> create(@RequestBody SchoolDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(schoolService.create(dto)));
    }

    @PutMapping("/api/schools/{id}")
    public ResponseEntity<ApiResponse<SchoolDto>> update(@PathVariable String id, @RequestBody SchoolDto dto) {
        return ResponseEntity.ok(ApiResponse.ok(schoolService.update(id, dto)));
    }

    @DeleteMapping("/api/schools/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String id) {
        schoolService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("School permanently deleted", null));
    }
}