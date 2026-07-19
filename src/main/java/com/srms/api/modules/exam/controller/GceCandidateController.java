package com.srms.api.modules.exam.controller;

import com.srms.api.common.ApiResponse;
import com.srms.api.common.BulkImportResult;
import com.srms.api.modules.exam.dto.GceCandidateDto;
import com.srms.api.modules.exam.entity.GceCandidate;
import com.srms.api.modules.exam.service.GceCandidateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/schools/{schoolId}/gce-candidates")
@RequiredArgsConstructor
public class GceCandidateController {
    private final GceCandidateService service;

    @GetMapping
    public ResponseEntity<ApiResponse<List<GceCandidate>>> list(@PathVariable String schoolId) {
        return ResponseEntity.ok(ApiResponse.ok(service.list(schoolId)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<GceCandidate>> getById(@PathVariable String schoolId, @PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.ok(service.findById(id, schoolId)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<GceCandidate>> create(@PathVariable String schoolId, @RequestBody GceCandidateDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(service.create(schoolId, dto)));
    }

    @PostMapping("/bulk")
    public ResponseEntity<ApiResponse<BulkImportResult>> bulkCreate(@PathVariable String schoolId, @RequestBody List<GceCandidateDto> dtos) {
        return ResponseEntity.ok(ApiResponse.ok(service.bulkCreate(schoolId, dtos)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<GceCandidate>> update(@PathVariable String schoolId, @PathVariable String id, @RequestBody GceCandidateDto dto) {
        return ResponseEntity.ok(ApiResponse.ok(service.update(id, schoolId, dto)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String schoolId, @PathVariable String id) {
        service.delete(id, schoolId);
        return ResponseEntity.ok(ApiResponse.ok("Deleted", null));
    }
}
