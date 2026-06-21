package com.srms.api.modules.health.controller;

import com.srms.api.common.ApiResponse;
import com.srms.api.modules.health.entity.HealthRecord;
import com.srms.api.modules.health.entity.HealthVisit;
import com.srms.api.modules.health.service.HealthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController @RequestMapping("/api/schools/{schoolId}/health") @RequiredArgsConstructor
public class HealthController {
    private final HealthService healthService;

    @GetMapping("/records") public ResponseEntity<ApiResponse<List<HealthRecord>>> getRecords(@PathVariable String schoolId) { return ResponseEntity.ok(ApiResponse.ok(healthService.getAllRecords(schoolId))); }
    @PostMapping("/records") public ResponseEntity<ApiResponse<HealthRecord>> saveRecord(@PathVariable String schoolId, @RequestBody HealthRecord record) { return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(healthService.saveRecord(schoolId, record))); }
    @GetMapping("/records/{id}") public ResponseEntity<ApiResponse<HealthRecord>> getRecord(@PathVariable String schoolId, @PathVariable String id) { return ResponseEntity.ok(ApiResponse.ok(healthService.getRecord(schoolId, id))); }
    @PutMapping("/records/{id}") public ResponseEntity<ApiResponse<HealthRecord>> updateRecord(@PathVariable String schoolId, @PathVariable String id, @RequestBody HealthRecord record) { record.setId(id); return ResponseEntity.ok(ApiResponse.ok(healthService.saveRecord(schoolId, record))); }
    @GetMapping("/records/student/{studentId}") public ResponseEntity<ApiResponse<HealthRecord>> getByStudent(@PathVariable String schoolId, @PathVariable String studentId) { return ResponseEntity.ok(ApiResponse.ok(healthService.getRecordByStudent(schoolId, studentId))); }

    @GetMapping("/visits") public ResponseEntity<ApiResponse<List<HealthVisit>>> getVisits(@PathVariable String schoolId) { return ResponseEntity.ok(ApiResponse.ok(healthService.getAllVisits(schoolId))); }
    @PostMapping("/visits") public ResponseEntity<ApiResponse<HealthVisit>> createVisit(@PathVariable String schoolId, @RequestBody HealthVisit visit) { return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(healthService.createVisit(schoolId, visit))); }
}
