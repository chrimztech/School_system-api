package com.srms.api.modules.development.controller;

import com.srms.api.common.ApiResponse;
import com.srms.api.modules.development.entity.StaffAppraisal;
import com.srms.api.modules.development.entity.StaffDevelopmentPlan;
import com.srms.api.modules.development.entity.StaffObservation;
import com.srms.api.modules.development.entity.TrainingRecord;
import com.srms.api.modules.development.service.DevelopmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @RequestMapping("/api/schools/{schoolId}/staff-development") @RequiredArgsConstructor
public class DevelopmentController {
    private final DevelopmentService developmentService;
    @GetMapping public ResponseEntity<ApiResponse<List<TrainingRecord>>> list(@PathVariable String schoolId) { return ResponseEntity.ok(ApiResponse.ok(developmentService.list(schoolId))); }
    @PostMapping public ResponseEntity<ApiResponse<TrainingRecord>> create(@PathVariable String schoolId, @RequestBody TrainingRecord t) { return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(developmentService.create(schoolId, t))); }
    @PutMapping("/{id}") public ResponseEntity<ApiResponse<TrainingRecord>> update(@PathVariable String schoolId, @PathVariable String id, @RequestBody TrainingRecord t) { return ResponseEntity.ok(ApiResponse.ok(developmentService.update(schoolId, id, t))); }

    @GetMapping("/appraisals") public ResponseEntity<ApiResponse<List<StaffAppraisal>>> appraisals(@PathVariable String schoolId) { return ResponseEntity.ok(ApiResponse.ok(developmentService.appraisals(schoolId))); }
    @PostMapping("/appraisals") public ResponseEntity<ApiResponse<StaffAppraisal>> createAppraisal(@PathVariable String schoolId, @RequestBody StaffAppraisal appraisal) { return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(developmentService.createAppraisal(schoolId, appraisal))); }
    @PatchMapping("/appraisals/{id}") public ResponseEntity<ApiResponse<StaffAppraisal>> updateAppraisal(@PathVariable String schoolId, @PathVariable String id, @RequestBody StaffAppraisal patch) { return ResponseEntity.ok(ApiResponse.ok(developmentService.updateAppraisal(schoolId, id, patch))); }

    @GetMapping("/observations") public ResponseEntity<ApiResponse<List<StaffObservation>>> observations(@PathVariable String schoolId) { return ResponseEntity.ok(ApiResponse.ok(developmentService.observations(schoolId))); }
    @PostMapping("/observations") public ResponseEntity<ApiResponse<StaffObservation>> createObservation(@PathVariable String schoolId, @RequestBody StaffObservation observation) { return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(developmentService.createObservation(schoolId, observation))); }

    @GetMapping("/pdps") public ResponseEntity<ApiResponse<List<StaffDevelopmentPlan>>> pdps(@PathVariable String schoolId) { return ResponseEntity.ok(ApiResponse.ok(developmentService.pdps(schoolId))); }
    @PostMapping("/pdps") public ResponseEntity<ApiResponse<StaffDevelopmentPlan>> createPdp(@PathVariable String schoolId, @RequestBody StaffDevelopmentPlan plan) { return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(developmentService.createPdp(schoolId, plan))); }
    @PatchMapping("/pdps/{id}") public ResponseEntity<ApiResponse<StaffDevelopmentPlan>> updatePdp(@PathVariable String schoolId, @PathVariable String id, @RequestBody StaffDevelopmentPlan patch) { return ResponseEntity.ok(ApiResponse.ok(developmentService.updatePdp(schoolId, id, patch))); }
}
