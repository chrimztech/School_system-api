package com.srms.api.modules.tuition.controller;

import com.srms.api.common.ApiResponse;
import com.srms.api.modules.tuition.dto.TuitionCenterSaveRequest;
import com.srms.api.modules.tuition.entity.TuitionCenter;
import com.srms.api.modules.tuition.service.TuitionCenterService;
import com.srms.api.security.RoleGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Manages the platform-wide tuition centre directory — one shared list every school's
 * performance-insight recommendations draw from, not owned by any single school. Super-admin
 * only, same as the rest of the platform workspace; see PerformanceInsightController for the
 * school-scoped, parent-facing read path that actually surfaces these to a family.
 */
@RestController
@RequestMapping("/api/platform/tuition-centers")
@RequiredArgsConstructor
public class TuitionCenterController {
    private final TuitionCenterService service;

    @GetMapping
    public ResponseEntity<ApiResponse<List<TuitionCenter>>> list(Authentication auth) {
        RoleGuard.requireSuperAdmin(auth);
        return ResponseEntity.ok(ApiResponse.ok(service.listAll()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<TuitionCenter>> create(@RequestBody TuitionCenterSaveRequest req, Authentication auth) {
        RoleGuard.requireSuperAdmin(auth);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(service.create(req, auth.getName())));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TuitionCenter>> update(@PathVariable String id, @RequestBody TuitionCenterSaveRequest req, Authentication auth) {
        RoleGuard.requireSuperAdmin(auth);
        return ResponseEntity.ok(ApiResponse.ok(service.update(id, req, auth.getName())));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String id, Authentication auth) {
        RoleGuard.requireSuperAdmin(auth);
        service.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Tuition centre deleted", null));
    }
}
