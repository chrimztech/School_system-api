package com.srms.api.modules.transport.controller;

import com.srms.api.common.ApiResponse;
import com.srms.api.exception.ForbiddenException;
import com.srms.api.modules.transport.entity.TransportEnrolment;
import com.srms.api.modules.transport.entity.TransportRoute;
import com.srms.api.modules.transport.entity.Vehicle;
import com.srms.api.modules.transport.service.TransportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** transport is "false" for PARENT — vehicles/routes are fleet config, not pupil-identifying,
 * so they're left open (same reasoning as fee-structures elsewhere), but enrolments name which
 * pupil rides which route/vehicle and has no per-student lookup to scope to instead, so it's
 * staff-only. */
@RestController
@RequestMapping("/api/schools/{schoolId}/transport")
@RequiredArgsConstructor
public class TransportController {

    private final TransportService transportService;

    private static String roleOf(Authentication auth) {
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst().map(a -> a.replaceFirst("^ROLE_", "")).orElse("");
    }

    @GetMapping("/vehicles")
    public ResponseEntity<ApiResponse<List<Vehicle>>> getVehicles(@PathVariable String schoolId) {
        return ResponseEntity.ok(ApiResponse.ok(transportService.getAllVehicles(schoolId)));
    }

    @PostMapping("/vehicles")
    public ResponseEntity<ApiResponse<Vehicle>> createVehicle(@PathVariable String schoolId, @RequestBody Vehicle vehicle) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(transportService.createVehicle(schoolId, vehicle)));
    }

    @PutMapping("/vehicles/{id}")
    public ResponseEntity<ApiResponse<Vehicle>> updateVehicle(@PathVariable String schoolId, @PathVariable String id, @RequestBody Vehicle vehicle) {
        return ResponseEntity.ok(ApiResponse.ok(transportService.updateVehicle(schoolId, id, vehicle)));
    }

    @DeleteMapping("/vehicles/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteVehicle(@PathVariable String schoolId, @PathVariable String id) {
        transportService.deleteVehicle(schoolId, id);
        return ResponseEntity.ok(ApiResponse.ok("Vehicle removed", null));
    }

    @GetMapping("/routes")
    public ResponseEntity<ApiResponse<List<TransportRoute>>> getRoutes(@PathVariable String schoolId) {
        return ResponseEntity.ok(ApiResponse.ok(transportService.getAllRoutes(schoolId)));
    }

    @PostMapping("/routes")
    public ResponseEntity<ApiResponse<TransportRoute>> createRoute(@PathVariable String schoolId, @RequestBody TransportRoute route) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(transportService.createRoute(schoolId, route)));
    }

    @GetMapping("/enrolments")
    public ResponseEntity<ApiResponse<List<TransportEnrolment>>> getEnrolments(@PathVariable String schoolId, Authentication auth) {
        if ("PARENT".equals(roleOf(auth))) {
            throw new ForbiddenException("Your role cannot access transport enrolments");
        }
        return ResponseEntity.ok(ApiResponse.ok(transportService.getAllEnrolments(schoolId)));
    }

    @PostMapping("/enrolments")
    public ResponseEntity<ApiResponse<TransportEnrolment>> createEnrolment(@PathVariable String schoolId, @RequestBody TransportEnrolment enrolment) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(transportService.createEnrolment(schoolId, enrolment)));
    }

    @PutMapping("/enrolments/{id}")
    public ResponseEntity<ApiResponse<TransportEnrolment>> updateEnrolment(@PathVariable String schoolId, @PathVariable String id, @RequestBody TransportEnrolment enrolment) {
        return ResponseEntity.ok(ApiResponse.ok(transportService.updateEnrolment(schoolId, id, enrolment)));
    }

    @DeleteMapping("/enrolments/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteEnrolment(@PathVariable String schoolId, @PathVariable String id) {
        transportService.deleteEnrolment(schoolId, id);
        return ResponseEntity.ok(ApiResponse.ok("Enrolment removed", null));
    }
}
