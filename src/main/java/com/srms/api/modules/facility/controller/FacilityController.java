package com.srms.api.modules.facility.controller;

import com.srms.api.common.ApiResponse;
import com.srms.api.modules.facility.entity.Asset;
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

    @GetMapping("/assets") public ResponseEntity<ApiResponse<List<Asset>>> listAssets(@PathVariable String schoolId) { return ResponseEntity.ok(ApiResponse.ok(facilityService.listAssets(schoolId))); }
    @PostMapping("/assets") public ResponseEntity<ApiResponse<Asset>> createAsset(@PathVariable String schoolId, @RequestBody Asset asset) { return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(facilityService.createAsset(schoolId, asset))); }
    @PutMapping("/assets/{id}") public ResponseEntity<ApiResponse<Asset>> updateAsset(@PathVariable String schoolId, @PathVariable String id, @RequestBody Asset asset) { return ResponseEntity.ok(ApiResponse.ok(facilityService.updateAsset(schoolId, id, asset))); }
    @DeleteMapping("/assets/{id}") public ResponseEntity<ApiResponse<Void>> deleteAsset(@PathVariable String schoolId, @PathVariable String id) { facilityService.deleteAsset(schoolId, id); return ResponseEntity.ok(ApiResponse.ok("Deleted", null)); }
}
