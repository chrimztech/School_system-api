package com.srms.api.modules.curriculum.controller;

import com.srms.api.common.ApiResponse;
import com.srms.api.modules.curriculum.dto.LessonPlanRequests;
import com.srms.api.modules.curriculum.entity.LessonPlan;
import com.srms.api.modules.curriculum.service.LessonPlanService;
import com.srms.api.security.RoleGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.srms.api.modules.curriculum.controller.CurriculumController.STAFF;
import static com.srms.api.modules.curriculum.controller.CurriculumController.requireRole;

@RestController
@RequestMapping("/api/schools/{schoolId}/lesson-plans")
@RequiredArgsConstructor
public class LessonPlanController {
    private final LessonPlanService service;

    @GetMapping
    public ResponseEntity<ApiResponse<List<LessonPlan>>> list(@PathVariable String schoolId,
                                                              @RequestParam(required = false) String classId,
                                                              Authentication auth) {
        requireRole(auth, STAFF);
        return ResponseEntity.ok(ApiResponse.ok(service.list(schoolId, classId, auth.getName(), RoleGuard.roleOf(auth))));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<LessonPlan>> create(@PathVariable String schoolId, @RequestBody LessonPlanRequests.Save req, Authentication auth) {
        requireRole(auth, STAFF);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(service.create(schoolId, req, auth.getName(), RoleGuard.roleOf(auth))));
    }

    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<LessonPlan>> generate(@PathVariable String schoolId, @RequestBody LessonPlanRequests.Generate req, Authentication auth) {
        requireRole(auth, STAFF);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(service.generate(schoolId, req, auth.getName(), RoleGuard.roleOf(auth))));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<LessonPlan>> update(@PathVariable String schoolId, @PathVariable String id,
                                                          @RequestBody LessonPlanRequests.Save req, Authentication auth) {
        requireRole(auth, STAFF);
        return ResponseEntity.ok(ApiResponse.ok(service.update(schoolId, id, req, auth.getName())));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String schoolId, @PathVariable String id, Authentication auth) {
        requireRole(auth, STAFF);
        service.delete(schoolId, id, auth.getName());
        return ResponseEntity.ok(ApiResponse.ok("Lesson plan deleted", null));
    }
}
