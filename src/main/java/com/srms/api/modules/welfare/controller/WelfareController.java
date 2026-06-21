package com.srms.api.modules.welfare.controller;

import com.srms.api.common.ApiResponse;
import com.srms.api.modules.welfare.entity.CounselingSession;
import com.srms.api.modules.welfare.entity.WelfareCase;
import com.srms.api.modules.welfare.service.WelfareService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @RequestMapping("/api/schools/{schoolId}/welfare") @RequiredArgsConstructor
public class WelfareController {
    private final WelfareService welfareService;

    @GetMapping("/cases") public ResponseEntity<ApiResponse<List<WelfareCase>>> listCases(@PathVariable String schoolId) { return ResponseEntity.ok(ApiResponse.ok(welfareService.listCases(schoolId))); }
    @PostMapping("/cases") public ResponseEntity<ApiResponse<WelfareCase>> createCase(@PathVariable String schoolId, @RequestBody WelfareCase c) { return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(welfareService.createCase(schoolId, c))); }
    @PutMapping("/cases/{id}") public ResponseEntity<ApiResponse<WelfareCase>> updateCase(@PathVariable String schoolId, @PathVariable String id, @RequestBody WelfareCase c) { return ResponseEntity.ok(ApiResponse.ok(welfareService.updateCase(schoolId, id, c))); }

    @GetMapping("/sessions") public ResponseEntity<ApiResponse<List<CounselingSession>>> listSessions(@PathVariable String schoolId) { return ResponseEntity.ok(ApiResponse.ok(welfareService.listSessions(schoolId))); }
    @PostMapping("/sessions") public ResponseEntity<ApiResponse<CounselingSession>> createSession(@PathVariable String schoolId, @RequestBody CounselingSession s) { return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(welfareService.createSession(schoolId, s))); }
}
