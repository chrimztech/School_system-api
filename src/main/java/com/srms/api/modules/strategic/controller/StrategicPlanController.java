package com.srms.api.modules.strategic.controller;

import com.srms.api.common.ApiResponse;
import com.srms.api.modules.strategic.entity.ActionItem;
import com.srms.api.modules.strategic.entity.StrategicGoal;
import com.srms.api.modules.strategic.entity.StrategicReview;
import com.srms.api.modules.strategic.service.StrategicPlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/schools/{schoolId}/strategic-plan")
@RequiredArgsConstructor
public class StrategicPlanController {
    private final StrategicPlanService service;

    @GetMapping("/goals") public ResponseEntity<ApiResponse<List<StrategicGoal>>> getGoals(@PathVariable String schoolId) { return ResponseEntity.ok(ApiResponse.ok(service.getGoals(schoolId))); }
    @PostMapping("/goals") public ResponseEntity<ApiResponse<StrategicGoal>> createGoal(@PathVariable String schoolId, @RequestBody StrategicGoal goal) { return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(service.createGoal(schoolId, goal))); }
    @PatchMapping("/goals/{id}") public ResponseEntity<ApiResponse<StrategicGoal>> updateGoal(@PathVariable String schoolId, @PathVariable String id, @RequestBody StrategicGoal patch) { return ResponseEntity.ok(ApiResponse.ok(service.updateGoal(schoolId, id, patch))); }

    @GetMapping("/actions") public ResponseEntity<ApiResponse<List<ActionItem>>> getActions(@PathVariable String schoolId) { return ResponseEntity.ok(ApiResponse.ok(service.getActions(schoolId))); }
    @PostMapping("/actions") public ResponseEntity<ApiResponse<ActionItem>> createAction(@PathVariable String schoolId, @RequestBody ActionItem item) { return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(service.createAction(schoolId, item))); }
    @PatchMapping("/actions/{id}") public ResponseEntity<ApiResponse<ActionItem>> updateAction(@PathVariable String schoolId, @PathVariable String id, @RequestBody ActionItem patch) { return ResponseEntity.ok(ApiResponse.ok(service.updateAction(schoolId, id, patch))); }

    @GetMapping("/reviews") public ResponseEntity<ApiResponse<List<StrategicReview>>> getReviews(@PathVariable String schoolId) { return ResponseEntity.ok(ApiResponse.ok(service.getReviews(schoolId))); }
    @PostMapping("/reviews") public ResponseEntity<ApiResponse<StrategicReview>> createReview(@PathVariable String schoolId, @RequestBody StrategicReview review) { return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(service.createReview(schoolId, review))); }
}
