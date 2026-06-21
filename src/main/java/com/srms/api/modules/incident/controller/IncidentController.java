package com.srms.api.modules.incident.controller;

import com.srms.api.common.ApiResponse;
import com.srms.api.modules.incident.entity.Incident;
import com.srms.api.modules.incident.service.IncidentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @RequestMapping("/api/schools/{schoolId}/incidents") @RequiredArgsConstructor
public class IncidentController {
    private final IncidentService incidentService;
    @GetMapping public ResponseEntity<ApiResponse<List<Incident>>> list(@PathVariable String schoolId) { return ResponseEntity.ok(ApiResponse.ok(incidentService.list(schoolId))); }
    @PostMapping public ResponseEntity<ApiResponse<Incident>> create(@PathVariable String schoolId, @RequestBody Incident i) { return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(incidentService.create(schoolId, i))); }
    @PutMapping("/{id}") public ResponseEntity<ApiResponse<Incident>> update(@PathVariable String schoolId, @PathVariable String id, @RequestBody Incident i) { return ResponseEntity.ok(ApiResponse.ok(incidentService.update(schoolId, id, i))); }
    @PatchMapping("/{id}/resolve") public ResponseEntity<ApiResponse<Void>> resolve(@PathVariable String schoolId, @PathVariable String id) { incidentService.resolve(schoolId, id); return ResponseEntity.ok(ApiResponse.ok("Resolved", null)); }
}
