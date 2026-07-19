package com.srms.api.modules.exam.controller;

import com.srms.api.common.ApiResponse;
import com.srms.api.modules.exam.entity.ExamCandidate;
import com.srms.api.modules.exam.service.ExamCandidateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/schools/{schoolId}/exams/{examId}/candidates")
@RequiredArgsConstructor
public class ExamCandidateController {
    private final ExamCandidateService service;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ExamCandidate>>> list(@PathVariable String schoolId, @PathVariable String examId) {
        return ResponseEntity.ok(ApiResponse.ok(service.list(schoolId, examId)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ExamCandidate>> addStudent(@PathVariable String schoolId, @PathVariable String examId, @RequestBody Map<String, String> body) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(service.addStudent(schoolId, examId, body.get("studentId"))));
    }

    @PostMapping("/from-class")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> addFromClass(@PathVariable String schoolId, @PathVariable String examId, @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(ApiResponse.ok(service.addFromClass(schoolId, examId, body.get("classId"))));
    }

    @PostMapping("/gce")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> addGceCandidates(@PathVariable String schoolId, @PathVariable String examId, @RequestBody Map<String, List<String>> body) {
        return ResponseEntity.ok(ApiResponse.ok(service.addGceCandidates(schoolId, examId, body.getOrDefault("gceCandidateIds", List.of()))));
    }

    @DeleteMapping("/{candidateId}")
    public ResponseEntity<ApiResponse<Void>> remove(@PathVariable String schoolId, @PathVariable String examId, @PathVariable String candidateId) {
        service.remove(schoolId, examId, candidateId);
        return ResponseEntity.ok(ApiResponse.ok("Removed", null));
    }
}
