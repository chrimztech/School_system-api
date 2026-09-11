package com.srms.api.modules.academic.controller;

import com.srms.api.common.ApiResponse;
import com.srms.api.modules.academic.dto.PromotionRequest;
import com.srms.api.modules.academic.dto.PromotionResult;
import com.srms.api.modules.academic.service.PromotionService;
import com.srms.api.security.RoleGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/** Bulk grade promotion moves every pupil in a class up a year — a school-wide, hard-to-reverse
 * action, so it's restricted the same way as the other school-account-manager-tier actions
 * (RoleGuard.requireSchoolAccountManager), not left open to any authenticated user. */
@RestController
@RequestMapping("/api/schools/{schoolId}/promotions")
@RequiredArgsConstructor
public class PromotionController {
    private final PromotionService promotionService;

    @PostMapping
    public ResponseEntity<ApiResponse<PromotionResult>> promote(@PathVariable String schoolId, @RequestBody PromotionRequest request, Authentication auth) {
        RoleGuard.requireSchoolAccountManager(auth);
        return ResponseEntity.ok(ApiResponse.ok(promotionService.promote(schoolId, request)));
    }
}
