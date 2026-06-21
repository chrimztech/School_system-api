package com.srms.api.modules.activities.controller;

import com.srms.api.common.ApiResponse;
import com.srms.api.modules.activities.entity.Activity;
import com.srms.api.modules.activities.entity.ActivityEnrolment;
import com.srms.api.modules.activities.service.ActivitiesService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController @RequestMapping("/api/schools/{schoolId}/activities") @RequiredArgsConstructor
public class ActivitiesController {
    private final ActivitiesService activitiesService;

    @GetMapping public ResponseEntity<ApiResponse<List<Activity>>> getAll(@PathVariable String schoolId) { return ResponseEntity.ok(ApiResponse.ok(activitiesService.getAll(schoolId))); }
    @PostMapping public ResponseEntity<ApiResponse<Activity>> create(@PathVariable String schoolId, @RequestBody Activity activity) { return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(activitiesService.create(schoolId, activity))); }
    @PutMapping("/{id}") public ResponseEntity<ApiResponse<Activity>> update(@PathVariable String schoolId, @PathVariable String id, @RequestBody Activity activity) { return ResponseEntity.ok(ApiResponse.ok(activitiesService.update(schoolId, id, activity))); }
    @DeleteMapping("/{id}") public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String schoolId, @PathVariable String id) { activitiesService.delete(schoolId, id); return ResponseEntity.ok(ApiResponse.ok("Deleted", null)); }

    @GetMapping("/{id}/enrolments") public ResponseEntity<ApiResponse<List<ActivityEnrolment>>> getEnrolments(@PathVariable String schoolId, @PathVariable String id) { return ResponseEntity.ok(ApiResponse.ok(activitiesService.getEnrolments(schoolId, id))); }
    @PostMapping("/{id}/enrolments") public ResponseEntity<ApiResponse<ActivityEnrolment>> enrol(@PathVariable String schoolId, @PathVariable String id, @RequestBody ActivityEnrolment enrolment) { enrolment.setActivityId(id); return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(activitiesService.enrol(schoolId, enrolment))); }
}
