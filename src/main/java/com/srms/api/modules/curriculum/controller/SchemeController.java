package com.srms.api.modules.curriculum.controller;

import com.srms.api.common.ApiResponse;
import com.srms.api.modules.curriculum.dto.SchemeRequests;
import com.srms.api.modules.curriculum.entity.SchemeOfWork;
import com.srms.api.modules.curriculum.entity.SchemeWeek;
import com.srms.api.modules.curriculum.service.SchemeService;
import com.srms.api.security.RoleGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.srms.api.modules.curriculum.controller.CurriculumController.CURRICULUM_MANAGERS;
import static com.srms.api.modules.curriculum.controller.CurriculumController.STAFF;
import static com.srms.api.modules.curriculum.controller.CurriculumController.requireRole;

@RestController
@RequestMapping("/api/schools/{schoolId}/schemes-of-work")
@RequiredArgsConstructor
public class SchemeController {
    private final SchemeService service;

    @GetMapping
    public ResponseEntity<ApiResponse<List<SchemeOfWork>>> list(@PathVariable String schoolId, Authentication auth) {
        requireRole(auth, STAFF);
        return ResponseEntity.ok(ApiResponse.ok(service.list(schoolId)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SchemeOfWork>> create(@PathVariable String schoolId, @RequestBody SchemeRequests.Create req, Authentication auth) {
        requireRole(auth, STAFF);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(service.create(schoolId, req, auth.getName())));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> get(@PathVariable String schoolId, @PathVariable String id, Authentication auth) {
        requireRole(auth, STAFF);
        return ResponseEntity.ok(ApiResponse.ok(Map.of(
                "scheme", service.get(schoolId, id),
                "weeks", service.weeks(schoolId, id))));
    }

    @PutMapping("/{id}/weeks")
    public ResponseEntity<ApiResponse<List<SchemeWeek>>> saveWeeks(@PathVariable String schoolId, @PathVariable String id,
                                                                   @RequestBody SchemeRequests.WeeksSave req, Authentication auth) {
        requireRole(auth, STAFF);
        return ResponseEntity.ok(ApiResponse.ok(service.replaceWeeks(schoolId, id, req.weeks(), auth.getName(), isReviewer(auth))));
    }

    @PostMapping("/{id}/populate")
    public ResponseEntity<ApiResponse<List<SchemeWeek>>> populate(@PathVariable String schoolId, @PathVariable String id, Authentication auth) {
        requireRole(auth, STAFF);
        return ResponseEntity.ok(ApiResponse.ok(service.populateFromCurriculum(schoolId, id, auth.getName(), isReviewer(auth))));
    }

    @PostMapping("/{id}/submit")
    public ResponseEntity<ApiResponse<SchemeOfWork>> submit(@PathVariable String schoolId, @PathVariable String id, Authentication auth) {
        requireRole(auth, STAFF);
        return ResponseEntity.ok(ApiResponse.ok(service.submit(schoolId, id, auth.getName())));
    }

    @PostMapping("/{id}/review")
    public ResponseEntity<ApiResponse<SchemeOfWork>> review(@PathVariable String schoolId, @PathVariable String id,
                                                            @RequestBody SchemeRequests.Review req, Authentication auth) {
        requireRole(auth, CURRICULUM_MANAGERS);
        return ResponseEntity.ok(ApiResponse.ok(service.review(schoolId, id, req, auth.getName())));
    }

    private static boolean isReviewer(Authentication auth) {
        return Set.of("SUPER_ADMIN", "SCHOOL_ADMIN", "PRINCIPAL", "DEPUTY_HEAD", "HOD").contains(RoleGuard.roleOf(auth));
    }
}
