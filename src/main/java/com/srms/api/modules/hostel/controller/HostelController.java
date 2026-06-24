package com.srms.api.modules.hostel.controller;

import com.srms.api.common.ApiResponse;
import com.srms.api.modules.hostel.entity.HostelAllocation;
import com.srms.api.modules.hostel.entity.HostelLeave;
import com.srms.api.modules.hostel.entity.HostelRoom;
import com.srms.api.modules.hostel.service.HostelService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController @RequestMapping("/api/schools/{schoolId}/hostel") @RequiredArgsConstructor
public class HostelController {
    private final HostelService hostelService;

    @GetMapping("/rooms") public ResponseEntity<ApiResponse<List<HostelRoom>>> getRooms(@PathVariable String schoolId) { return ResponseEntity.ok(ApiResponse.ok(hostelService.getRooms(schoolId))); }
    @PostMapping("/rooms") public ResponseEntity<ApiResponse<HostelRoom>> createRoom(@PathVariable String schoolId, @RequestBody HostelRoom room) { return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(hostelService.createRoom(schoolId, room))); }
    @PutMapping("/rooms/{id}") public ResponseEntity<ApiResponse<HostelRoom>> updateRoom(@PathVariable String schoolId, @PathVariable String id, @RequestBody HostelRoom room) { return ResponseEntity.ok(ApiResponse.ok(hostelService.updateRoom(schoolId, id, room))); }
    @DeleteMapping("/rooms/{id}") public ResponseEntity<ApiResponse<Void>> deleteRoom(@PathVariable String schoolId, @PathVariable String id) { hostelService.deleteRoom(schoolId, id); return ResponseEntity.ok(ApiResponse.ok("Deleted", null)); }

    @GetMapping("/allocations") public ResponseEntity<ApiResponse<List<HostelAllocation>>> getAllocations(@PathVariable String schoolId) { return ResponseEntity.ok(ApiResponse.ok(hostelService.getAllocations(schoolId))); }
    @PostMapping("/allocations") public ResponseEntity<ApiResponse<HostelAllocation>> allocate(@PathVariable String schoolId, @RequestBody HostelAllocation allocation) { return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(hostelService.allocate(schoolId, allocation))); }
    @PutMapping("/allocations/{id}/vacate") public ResponseEntity<ApiResponse<HostelAllocation>> vacate(@PathVariable String schoolId, @PathVariable String id) { return ResponseEntity.ok(ApiResponse.ok(hostelService.vacate(schoolId, id))); }

    @GetMapping("/leaves") public ResponseEntity<ApiResponse<List<HostelLeave>>> getLeaves(@PathVariable String schoolId) { return ResponseEntity.ok(ApiResponse.ok(hostelService.getLeaves(schoolId))); }
    @PostMapping("/leaves") public ResponseEntity<ApiResponse<HostelLeave>> createLeave(@PathVariable String schoolId, @RequestBody HostelLeave leave) { return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(hostelService.createLeave(schoolId, leave))); }
    @PatchMapping("/leaves/{id}/status") public ResponseEntity<ApiResponse<HostelLeave>> updateLeaveStatus(@PathVariable String schoolId, @PathVariable String id, @RequestParam String status) { return ResponseEntity.ok(ApiResponse.ok(hostelService.updateLeaveStatus(schoolId, id, status))); }
    @PatchMapping("/allocations/{id}/sign-in") public ResponseEntity<ApiResponse<HostelAllocation>> updateSignIn(@PathVariable String schoolId, @PathVariable String id, @RequestParam String status) { return ResponseEntity.ok(ApiResponse.ok(hostelService.updateSignInStatus(schoolId, id, status))); }
}
