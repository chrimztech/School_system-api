package com.srms.api.modules.assessment.controller;
import com.srms.api.common.ApiResponse;
import com.srms.api.modules.assessment.entity.GradeWeightConfig;
import com.srms.api.modules.assessment.service.GradeWeightConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/schools/{schoolId}/grade-weights") @RequiredArgsConstructor
public class GradeWeightConfigController {
    private final GradeWeightConfigService service;

    @GetMapping
    public ResponseEntity<ApiResponse<GradeWeightConfig>> get(@PathVariable String schoolId) {
        return ResponseEntity.ok(ApiResponse.ok(service.get(schoolId)));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<GradeWeightConfig>> update(@PathVariable String schoolId, @RequestBody GradeWeightConfig dto) {
        return ResponseEntity.ok(ApiResponse.ok(service.upsert(schoolId, dto.getCaWeight(), dto.getMidtermWeight(), dto.getExamWeight())));
    }
}
