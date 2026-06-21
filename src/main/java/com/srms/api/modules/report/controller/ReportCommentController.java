package com.srms.api.modules.report.controller;

import com.srms.api.common.ApiResponse;
import com.srms.api.modules.report.entity.ReportComment;
import com.srms.api.modules.report.service.ReportCommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/schools/{schoolId}/report-comments")
@RequiredArgsConstructor
public class ReportCommentController {
    private final ReportCommentService service;

    @GetMapping("/student/{studentId}")
    public ResponseEntity<ApiResponse<ReportComment>> get(
            @PathVariable String schoolId,
            @PathVariable String studentId,
            @RequestParam String term,
            @RequestParam String academicYear) {
        return service.find(schoolId, studentId, term, academicYear)
                .map(rc -> ResponseEntity.ok(ApiResponse.ok(rc)))
                .orElse(ResponseEntity.ok(ApiResponse.ok(null)));
    }

    @PutMapping("/student/{studentId}")
    public ResponseEntity<ApiResponse<ReportComment>> upsert(
            @PathVariable String schoolId,
            @PathVariable String studentId,
            @RequestParam String term,
            @RequestParam String academicYear,
            @RequestBody Map<String, String> body) {
        ReportComment rc = service.upsert(schoolId, studentId, term, academicYear,
                body.get("teacherComment"), body.get("headComment"));
        return ResponseEntity.ok(ApiResponse.ok(rc));
    }
}
