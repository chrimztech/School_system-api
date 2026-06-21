package com.srms.api.modules.exam.controller;

import com.srms.api.common.ApiResponse;
import com.srms.api.modules.exam.entity.ExamPaper;
import com.srms.api.modules.exam.service.ExamService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @RequestMapping("/api/schools/{schoolId}/exams") @RequiredArgsConstructor
public class ExamController {
    private final ExamService examService;
    @GetMapping public ResponseEntity<ApiResponse<List<ExamPaper>>> list(@PathVariable String schoolId) { return ResponseEntity.ok(ApiResponse.ok(examService.list(schoolId))); }
    @PostMapping public ResponseEntity<ApiResponse<ExamPaper>> create(@PathVariable String schoolId, @RequestBody ExamPaper paper) { return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(examService.create(schoolId, paper))); }
    @PutMapping("/{id}") public ResponseEntity<ApiResponse<ExamPaper>> update(@PathVariable String schoolId, @PathVariable String id, @RequestBody ExamPaper paper) { return ResponseEntity.ok(ApiResponse.ok(examService.update(schoolId, id, paper))); }
    @DeleteMapping("/{id}") public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String schoolId, @PathVariable String id) { examService.delete(schoolId, id); return ResponseEntity.ok(ApiResponse.ok("Deleted", null)); }
}
