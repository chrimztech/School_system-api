package com.srms.api.modules.hr.controller;

import com.srms.api.common.ApiResponse;
import com.srms.api.modules.hr.entity.LeaveRequest;
import com.srms.api.modules.hr.entity.StaffRecord;
import com.srms.api.modules.hr.service.HrService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/schools/{schoolId}/hr")
@RequiredArgsConstructor
public class HrController {

    private final HrService hrService;

    @GetMapping("/staff")
    public ResponseEntity<ApiResponse<List<StaffRecord>>> getStaff(@PathVariable String schoolId) {
        return ResponseEntity.ok(ApiResponse.ok(hrService.getAllStaff(schoolId)));
    }

    @PostMapping("/staff")
    public ResponseEntity<ApiResponse<StaffRecord>> createStaff(@PathVariable String schoolId, @RequestBody StaffRecord record) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(hrService.createStaff(schoolId, record)));
    }

    @PutMapping("/staff/{id}")
    public ResponseEntity<ApiResponse<StaffRecord>> updateStaff(@PathVariable String schoolId, @PathVariable String id, @RequestBody StaffRecord record) {
        return ResponseEntity.ok(ApiResponse.ok(hrService.updateStaff(schoolId, id, record)));
    }

    @DeleteMapping("/staff/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteStaff(@PathVariable String schoolId, @PathVariable String id) {
        hrService.deleteStaff(schoolId, id);
        return ResponseEntity.ok(ApiResponse.ok("Staff record terminated", null));
    }

    @GetMapping("/leave")
    public ResponseEntity<ApiResponse<List<LeaveRequest>>> getLeave(@PathVariable String schoolId) {
        return ResponseEntity.ok(ApiResponse.ok(hrService.getAllLeave(schoolId)));
    }

    @PostMapping("/leave")
    public ResponseEntity<ApiResponse<LeaveRequest>> submitLeave(@PathVariable String schoolId, @RequestBody LeaveRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(hrService.submitLeave(schoolId, request)));
    }

    @PutMapping("/leave/{id}/approve")
    public ResponseEntity<ApiResponse<LeaveRequest>> approveLeave(@PathVariable String schoolId, @PathVariable String id, @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(ApiResponse.ok(hrService.approveLeave(schoolId, id, body.getOrDefault("approvedBy", "Admin"))));
    }

    @PutMapping("/leave/{id}/reject")
    public ResponseEntity<ApiResponse<LeaveRequest>> rejectLeave(@PathVariable String schoolId, @PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.ok(hrService.rejectLeave(schoolId, id)));
    }
}
