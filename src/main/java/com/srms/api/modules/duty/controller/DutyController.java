package com.srms.api.modules.duty.controller;

import com.srms.api.common.ApiResponse;
import com.srms.api.modules.duty.entity.DutyAssignment;
import com.srms.api.modules.duty.service.DutyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @RequestMapping("/api/schools/{schoolId}/duty-roster") @RequiredArgsConstructor
public class DutyController {
    private final DutyService dutyService;
    @GetMapping public ResponseEntity<ApiResponse<List<DutyAssignment>>> list(@PathVariable String schoolId) { return ResponseEntity.ok(ApiResponse.ok(dutyService.list(schoolId))); }
    @PostMapping public ResponseEntity<ApiResponse<DutyAssignment>> create(@PathVariable String schoolId, @RequestBody DutyAssignment d) { return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(dutyService.create(schoolId, d))); }
    @DeleteMapping("/{id}") public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String schoolId, @PathVariable String id) { dutyService.delete(schoolId, id); return ResponseEntity.ok(ApiResponse.ok("Deleted", null)); }
}
