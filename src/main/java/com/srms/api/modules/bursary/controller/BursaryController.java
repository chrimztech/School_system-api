package com.srms.api.modules.bursary.controller;

import com.srms.api.common.ApiResponse;
import com.srms.api.modules.bursary.entity.BursaryApplication;
import com.srms.api.modules.bursary.entity.BursaryAward;
import com.srms.api.modules.bursary.entity.BursaryRenewal;
import com.srms.api.modules.bursary.service.BursaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @RequestMapping("/api/schools/{schoolId}/bursaries") @RequiredArgsConstructor
public class BursaryController {
    private final BursaryService bursaryService;
    @GetMapping public ResponseEntity<ApiResponse<List<BursaryAward>>> list(@PathVariable String schoolId) { return ResponseEntity.ok(ApiResponse.ok(bursaryService.list(schoolId))); }
    @PostMapping public ResponseEntity<ApiResponse<BursaryAward>> create(@PathVariable String schoolId, @RequestBody BursaryAward b) { return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(bursaryService.create(schoolId, b))); }
    @PutMapping("/{id}") public ResponseEntity<ApiResponse<BursaryAward>> update(@PathVariable String schoolId, @PathVariable String id, @RequestBody BursaryAward b) { return ResponseEntity.ok(ApiResponse.ok(bursaryService.update(schoolId, id, b))); }

    @GetMapping("/applications") public ResponseEntity<ApiResponse<List<BursaryApplication>>> applications(@PathVariable String schoolId) { return ResponseEntity.ok(ApiResponse.ok(bursaryService.applications(schoolId))); }
    @PostMapping("/applications") public ResponseEntity<ApiResponse<BursaryApplication>> createApplication(@PathVariable String schoolId, @RequestBody BursaryApplication application) { return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(bursaryService.createApplication(schoolId, application))); }
    @PatchMapping("/applications/{id}") public ResponseEntity<ApiResponse<BursaryApplication>> updateApplication(@PathVariable String schoolId, @PathVariable String id, @RequestBody BursaryApplication patch) { return ResponseEntity.ok(ApiResponse.ok(bursaryService.updateApplication(schoolId, id, patch))); }

    @GetMapping("/renewals") public ResponseEntity<ApiResponse<List<BursaryRenewal>>> renewals(@PathVariable String schoolId) { return ResponseEntity.ok(ApiResponse.ok(bursaryService.renewals(schoolId))); }
    @PostMapping("/renewals") public ResponseEntity<ApiResponse<BursaryRenewal>> createRenewal(@PathVariable String schoolId, @RequestBody BursaryRenewal renewal) { return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(bursaryService.createRenewal(schoolId, renewal))); }
    @PatchMapping("/renewals/{id}") public ResponseEntity<ApiResponse<BursaryRenewal>> updateRenewal(@PathVariable String schoolId, @PathVariable String id, @RequestBody BursaryRenewal patch) { return ResponseEntity.ok(ApiResponse.ok(bursaryService.updateRenewal(schoolId, id, patch))); }
}
