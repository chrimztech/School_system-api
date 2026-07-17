package com.srms.api.modules.assessment.controller;
import com.srms.api.common.ApiResponse;
import com.srms.api.common.PageRequestUtil;
import com.srms.api.common.PageResponse;
import com.srms.api.modules.assessment.entity.Assessment;
import com.srms.api.modules.assessment.entity.AssessmentResult;
import com.srms.api.modules.assessment.service.AssessmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
@RestController @RequestMapping("/api/schools/{schoolId}/assessments") @RequiredArgsConstructor
public class AssessmentController {
    private final AssessmentService assessmentService;
    @GetMapping public ResponseEntity<ApiResponse<List<Assessment>>> getAll(@PathVariable String schoolId, @RequestParam(required = false) String term, @RequestParam(required = false) String academicYear) { return ResponseEntity.ok(ApiResponse.ok(assessmentService.findAll(schoolId, term, academicYear))); }
    @GetMapping("/{id}") public ResponseEntity<ApiResponse<Assessment>> getById(@PathVariable String schoolId, @PathVariable String id) { return ResponseEntity.ok(ApiResponse.ok(assessmentService.findById(id, schoolId))); }
    @PostMapping public ResponseEntity<ApiResponse<Assessment>> create(@PathVariable String schoolId, @RequestBody Assessment dto) { return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(assessmentService.create(schoolId, dto))); }
    @PutMapping("/{id}") public ResponseEntity<ApiResponse<Assessment>> update(@PathVariable String schoolId, @PathVariable String id, @RequestBody Assessment dto) { return ResponseEntity.ok(ApiResponse.ok(assessmentService.update(id, schoolId, dto))); }
    @GetMapping("/{id}/results") public ResponseEntity<ApiResponse<List<AssessmentResult>>> getResults(@PathVariable String schoolId, @PathVariable String id) { return ResponseEntity.ok(ApiResponse.ok(assessmentService.getResults(id))); }
    @PostMapping("/{id}/results") public ResponseEntity<ApiResponse<AssessmentResult>> saveResult(@PathVariable String schoolId, @PathVariable String id, @RequestBody AssessmentResult result) { result.setSchoolId(schoolId); result.setAssessmentId(id); return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(assessmentService.saveResult(result))); }
    @PostMapping("/{id}/results/bulk") public ResponseEntity<ApiResponse<List<AssessmentResult>>> saveResultsBulk(@PathVariable String schoolId, @PathVariable String id, @RequestBody List<AssessmentResult> results) { return ResponseEntity.ok(ApiResponse.ok(assessmentService.saveResultsBulk(id, schoolId, results))); }
    @GetMapping("/student/{studentId}")
    public ResponseEntity<ApiResponse<?>> getStudentResults(
            @PathVariable String schoolId, @PathVariable String studentId,
            @RequestParam(required = false) Integer page, @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sortBy, @RequestParam(required = false) String sortDir) {
        Pageable pageable = PageRequestUtil.build(page, size, sortBy, sortDir);
        if (pageable == null) return ResponseEntity.ok(ApiResponse.ok(assessmentService.getStudentResults(schoolId, studentId)));
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.of(assessmentService.getStudentResultsPaged(schoolId, studentId, pageable))));
    }
    @GetMapping("/student/{studentId}/enriched") public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getEnrichedStudentResults(@PathVariable String schoolId, @PathVariable String studentId) { return ResponseEntity.ok(ApiResponse.ok(assessmentService.getEnrichedStudentResults(schoolId, studentId))); }
}
