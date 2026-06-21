package com.srms.api.modules.assessment.controller;
import com.srms.api.common.ApiResponse;
import com.srms.api.modules.assessment.entity.Assessment;
import com.srms.api.modules.assessment.entity.AssessmentResult;
import com.srms.api.modules.assessment.service.AssessmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController @RequestMapping("/api/schools/{schoolId}/assessments") @RequiredArgsConstructor
public class AssessmentController {
    private final AssessmentService assessmentService;
    @GetMapping public ResponseEntity<ApiResponse<List<Assessment>>> getAll(@PathVariable String schoolId) { return ResponseEntity.ok(ApiResponse.ok(assessmentService.findAll(schoolId))); }
    @GetMapping("/{id}") public ResponseEntity<ApiResponse<Assessment>> getById(@PathVariable String schoolId, @PathVariable String id) { return ResponseEntity.ok(ApiResponse.ok(assessmentService.findById(id, schoolId))); }
    @PostMapping public ResponseEntity<ApiResponse<Assessment>> create(@PathVariable String schoolId, @RequestBody Assessment dto) { return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(assessmentService.create(schoolId, dto))); }
    @PutMapping("/{id}") public ResponseEntity<ApiResponse<Assessment>> update(@PathVariable String schoolId, @PathVariable String id, @RequestBody Assessment dto) { return ResponseEntity.ok(ApiResponse.ok(assessmentService.update(id, schoolId, dto))); }
    @GetMapping("/{id}/results") public ResponseEntity<ApiResponse<List<AssessmentResult>>> getResults(@PathVariable String schoolId, @PathVariable String id) { return ResponseEntity.ok(ApiResponse.ok(assessmentService.getResults(id))); }
    @PostMapping("/{id}/results") public ResponseEntity<ApiResponse<AssessmentResult>> saveResult(@PathVariable String schoolId, @PathVariable String id, @RequestBody AssessmentResult result) { result.setSchoolId(schoolId); result.setAssessmentId(id); return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(assessmentService.saveResult(result))); }
    @GetMapping("/student/{studentId}") public ResponseEntity<ApiResponse<List<AssessmentResult>>> getStudentResults(@PathVariable String schoolId, @PathVariable String studentId) { return ResponseEntity.ok(ApiResponse.ok(assessmentService.getStudentResults(schoolId, studentId))); }
}
