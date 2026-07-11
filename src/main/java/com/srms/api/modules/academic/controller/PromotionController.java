package com.srms.api.modules.academic.controller;

import com.srms.api.common.ApiResponse;
import com.srms.api.modules.academic.dto.PromotionRequest;
import com.srms.api.modules.academic.dto.PromotionResult;
import com.srms.api.modules.academic.service.PromotionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/schools/{schoolId}/promotions")
@RequiredArgsConstructor
public class PromotionController {
    private final PromotionService promotionService;

    @PostMapping
    public ResponseEntity<ApiResponse<PromotionResult>> promote(@PathVariable String schoolId, @RequestBody PromotionRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(promotionService.promote(schoolId, request)));
    }
}
