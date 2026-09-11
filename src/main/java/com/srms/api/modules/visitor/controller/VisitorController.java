package com.srms.api.modules.visitor.controller;

import com.srms.api.common.ApiResponse;
import com.srms.api.modules.visitor.entity.VisitorLog;
import com.srms.api.modules.visitor.service.VisitorService;
import com.srms.api.security.RoleGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/** "visitor-log" is "true" only for leadership in the frontend's own access matrix, false for
 * every other role — a front-desk security log, not something a teacher/parent should browse. */
@RestController @RequestMapping("/api/schools/{schoolId}/visitors") @RequiredArgsConstructor
public class VisitorController {
    private final VisitorService visitorService;
    @GetMapping public ResponseEntity<ApiResponse<List<VisitorLog>>> getAll(@PathVariable String schoolId, Authentication auth) { RoleGuard.requireSchoolAccountManager(auth); return ResponseEntity.ok(ApiResponse.ok(visitorService.getAll(schoolId))); }
    @PostMapping public ResponseEntity<ApiResponse<VisitorLog>> checkIn(@PathVariable String schoolId, @RequestBody VisitorLog log, Authentication auth) { RoleGuard.requireSchoolAccountManager(auth); return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(visitorService.checkIn(schoolId, log))); }
    @PutMapping("/{id}/checkout") public ResponseEntity<ApiResponse<VisitorLog>> checkOut(@PathVariable String schoolId, @PathVariable String id, Authentication auth) { RoleGuard.requireSchoolAccountManager(auth); return ResponseEntity.ok(ApiResponse.ok(visitorService.checkOut(schoolId, id))); }
}
