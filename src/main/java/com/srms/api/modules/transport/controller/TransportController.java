package com.srms.api.modules.transport.controller;

import com.srms.api.common.ApiResponse;
import com.srms.api.modules.transport.entity.TransportEnrolment;
import com.srms.api.modules.transport.entity.TransportRoute;
import com.srms.api.modules.transport.entity.Vehicle;
import com.srms.api.modules.transport.service.TransportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/schools/{schoolId}/transport")
@RequiredArgsConstructor
public class TransportController {

    private final TransportService transportService;

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
    public ResponseEntity<ApiResponse<List<TransportEnrolment>>> getEnrolments(@PathVariable String schoolId) {
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
