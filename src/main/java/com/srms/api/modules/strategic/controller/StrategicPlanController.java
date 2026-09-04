package com.srms.api.modules.strategic.controller;

import com.srms.api.common.ApiResponse;
import com.srms.api.exception.ForbiddenException;
import com.srms.api.modules.strategic.entity.ActionItem;
import com.srms.api.modules.strategic.entity.StrategicGoal;
import com.srms.api.modules.strategic.entity.StrategicReview;
import com.srms.api.modules.strategic.service.StrategicPlanService;
import com.srms.api.security.ModuleAccessService;
import com.srms.api.security.RoleGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Set;

// Reads stay open — the frontend's "strategic-plan" module gives teacher/finance "read" access —
// but writes are school-leadership only, the only roles the matrix marks as full access.
@RestController
@RequestMapping("/api/schools/{schoolId}/strategic-plan")
@RequiredArgsConstructor
public class StrategicPlanController {
    private final StrategicPlanService service;
    private final ModuleAccessService moduleAccessService;

    // Mirrors RoleGuard's own (private) SCHOOL_ACCOUNT_MANAGERS set.
    private static final Set<String> SCHOOL_ACCOUNT_MANAGERS = Set.of(
            "SUPER_ADMIN", "SCHOOL_ADMIN", "PRINCIPAL", "DEPUTY_HEAD");

    @GetMapping("/goals") public ResponseEntity<ApiResponse<List<StrategicGoal>>> getGoals(@PathVariable String schoolId) { return ResponseEntity.ok(ApiResponse.ok(service.getGoals(schoolId))); }
    @PostMapping("/goals") public ResponseEntity<ApiResponse<StrategicGoal>> createGoal(@PathVariable String schoolId, @RequestBody StrategicGoal goal, Authentication auth) { requireManage(schoolId, auth); return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(service.createGoal(schoolId, goal))); }
    @PatchMapping("/goals/{id}") public ResponseEntity<ApiResponse<StrategicGoal>> updateGoal(@PathVariable String schoolId, @PathVariable String id, @RequestBody StrategicGoal patch, Authentication auth) { requireManage(schoolId, auth); return ResponseEntity.ok(ApiResponse.ok(service.updateGoal(schoolId, id, patch))); }

    @GetMapping("/actions") public ResponseEntity<ApiResponse<List<ActionItem>>> getActions(@PathVariable String schoolId) { return ResponseEntity.ok(ApiResponse.ok(service.getActions(schoolId))); }
    @PostMapping("/actions") public ResponseEntity<ApiResponse<ActionItem>> createAction(@PathVariable String schoolId, @RequestBody ActionItem item, Authentication auth) { requireManage(schoolId, auth); return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(service.createAction(schoolId, item))); }
    @PatchMapping("/actions/{id}") public ResponseEntity<ApiResponse<ActionItem>> updateAction(@PathVariable String schoolId, @PathVariable String id, @RequestBody ActionItem patch, Authentication auth) { requireManage(schoolId, auth); return ResponseEntity.ok(ApiResponse.ok(service.updateAction(schoolId, id, patch))); }

    @GetMapping("/reviews") public ResponseEntity<ApiResponse<List<StrategicReview>>> getReviews(@PathVariable String schoolId) { return ResponseEntity.ok(ApiResponse.ok(service.getReviews(schoolId))); }
    @PostMapping("/reviews") public ResponseEntity<ApiResponse<StrategicReview>> createReview(@PathVariable String schoolId, @RequestBody StrategicReview review, Authentication auth) { requireManage(schoolId, auth); return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(service.createReview(schoolId, review))); }

    private void requireManage(String schoolId, Authentication auth) {
        if (!moduleAccessService.isAllowed(schoolId, auth, "strategic-plan", "full", SCHOOL_ACCOUNT_MANAGERS.contains(RoleGuard.roleOf(auth)))) {
            throw new ForbiddenException("Your role cannot manage school accounts or permissions");
        }
    }
}
