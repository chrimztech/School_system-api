package com.srms.api.modules.facility.controller;

import com.srms.api.common.ApiResponse;
import com.srms.api.modules.facility.entity.WorkOrder;
import com.srms.api.modules.facility.service.FacilityService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @RequestMapping("/api/schools/{schoolId}/facilities") @RequiredArgsConstructor
public class FacilityController {
    private final FacilityService facilityService;
    @GetMapping public ResponseEntity<ApiResponse<List<WorkOrder>>> list(@PathVariable String schoolId) { return ResponseEntity.ok(ApiResponse.ok(facilityService.list(schoolId))); }
    @PostMapping public ResponseEntity<ApiResponse<WorkOrder>> create(@PathVariable String schoolId, @RequestBody WorkOrder wo) { return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(facilityService.create(schoolId, wo))); }
    @PutMapping("/{id}") public ResponseEntity<ApiResponse<WorkOrder>> update(@PathVariable String schoolId, @PathVariable String id, @RequestBody WorkOrder wo) { return ResponseEntity.ok(ApiResponse.ok(facilityService.update(schoolId, id, wo))); }
    @PatchMapping("/{id}/close") public ResponseEntity<ApiResponse<Void>> close(@PathVariable String schoolId, @PathVariable String id) { facilityService.close(schoolId, id); return ResponseEntity.ok(ApiResponse.ok("Closed", null)); }
}
