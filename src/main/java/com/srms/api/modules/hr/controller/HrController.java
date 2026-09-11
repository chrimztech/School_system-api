package com.srms.api.modules.hr.controller;

import com.srms.api.common.ApiResponse;
import com.srms.api.exception.ForbiddenException;
import com.srms.api.modules.hr.entity.LeaveRequest;
import com.srms.api.modules.hr.entity.StaffRecord;
import com.srms.api.modules.hr.service.HrService;
import com.srms.api.security.ModuleAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

/** Staff salary/personal records and leave — "hr" is "true" only for FINANCE and leadership in
 * the frontend's own access matrix, false for TEACHER/HOD/CAREER_GUIDANCE/PARENT. */
@RestController
@RequestMapping("/api/schools/{schoolId}/hr")
@RequiredArgsConstructor
public class HrController {

    private final HrService hrService;
    private final ModuleAccessService moduleAccessService;

    private static final Set<String> FULL_ROLES = Set.of(
            "SUPER_ADMIN", "SCHOOL_ADMIN", "PRINCIPAL", "DEPUTY_HEAD", "FINANCE");

    private void requireFull(String schoolId, Authentication auth) {
        String role = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst().map(a -> a.replaceFirst("^ROLE_", "")).orElse("");
        if (!moduleAccessService.isAllowed(schoolId, auth, "hr", "full", FULL_ROLES.contains(role))) {
            throw new ForbiddenException("Your role cannot access HR records");
        }
    }

    @GetMapping("/staff")
    public ResponseEntity<ApiResponse<List<StaffRecord>>> getStaff(@PathVariable String schoolId, Authentication auth) {
        requireFull(schoolId, auth);
        return ResponseEntity.ok(ApiResponse.ok(hrService.getAllStaff(schoolId)));
    }

    @PostMapping("/staff")
    public ResponseEntity<ApiResponse<StaffRecord>> createStaff(@PathVariable String schoolId, @RequestBody StaffRecord record, Authentication auth) {
        requireFull(schoolId, auth);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(hrService.createStaff(schoolId, record)));
    }

    @PutMapping("/staff/{id}")
    public ResponseEntity<ApiResponse<StaffRecord>> updateStaff(@PathVariable String schoolId, @PathVariable String id, @RequestBody StaffRecord record, Authentication auth) {
        requireFull(schoolId, auth);
        return ResponseEntity.ok(ApiResponse.ok(hrService.updateStaff(schoolId, id, record)));
    }

    @DeleteMapping("/staff/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteStaff(@PathVariable String schoolId, @PathVariable String id, Authentication auth) {
        requireFull(schoolId, auth);
        hrService.deleteStaff(schoolId, id);
        return ResponseEntity.ok(ApiResponse.ok("Staff record terminated", null));
    }

    @GetMapping("/leave")
    public ResponseEntity<ApiResponse<List<LeaveRequest>>> getLeave(@PathVariable String schoolId, Authentication auth) {
        requireFull(schoolId, auth);
        return ResponseEntity.ok(ApiResponse.ok(hrService.getAllLeave(schoolId)));
    }

    @PostMapping("/leave")
    public ResponseEntity<ApiResponse<LeaveRequest>> submitLeave(@PathVariable String schoolId, @RequestBody LeaveRequest request, Authentication auth) {
        requireFull(schoolId, auth);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(hrService.submitLeave(schoolId, request)));
    }

    @PutMapping("/leave/{id}/approve")
    public ResponseEntity<ApiResponse<LeaveRequest>> approveLeave(@PathVariable String schoolId, @PathVariable String id, @RequestBody Map<String, String> body, Authentication auth) {
        requireFull(schoolId, auth);
        return ResponseEntity.ok(ApiResponse.ok(hrService.approveLeave(schoolId, id, body.getOrDefault("approvedBy", "Admin"))));
    }

    @PutMapping("/leave/{id}/reject")
    public ResponseEntity<ApiResponse<LeaveRequest>> rejectLeave(@PathVariable String schoolId, @PathVariable String id, Authentication auth) {
        requireFull(schoolId, auth);
        return ResponseEntity.ok(ApiResponse.ok(hrService.rejectLeave(schoolId, id)));
    }
}
