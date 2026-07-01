package com.srms.api.modules.assessment.controller;

import com.srms.api.common.ApiResponse;
import com.srms.api.modules.assessment.entity.TermGrade;
import com.srms.api.modules.assessment.service.TermGradeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/schools/{schoolId}/term-grades")
@RequiredArgsConstructor
public class TermGradeController {
    private final TermGradeService termGradeService;

    @PostMapping("/compute")
    public ResponseEntity<ApiResponse<List<TermGrade>>> compute(@PathVariable String schoolId, @RequestBody Map<String, String> body) {
        List<TermGrade> result = termGradeService.compute(schoolId, body.get("classId"), body.get("subjectName"),
                body.get("term"), body.get("academicYear"));
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @PatchMapping("/{id}/publish")
    public ResponseEntity<ApiResponse<TermGrade>> publish(@PathVariable String schoolId, @PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.ok(termGradeService.publish(id, schoolId)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<TermGrade>>> history(
            @PathVariable String schoolId,
            @RequestParam String studentId,
            @RequestParam String academicYear,
            @RequestParam(defaultValue = "false") boolean publishedOnly) {
        return ResponseEntity.ok(ApiResponse.ok(
                termGradeService.getHistory(schoolId, studentId, academicYear, !publishedOnly)));
    }

    @GetMapping("/class-stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> classStats(
            @PathVariable String schoolId,
            @RequestParam String classId,
            @RequestParam String subjectName,
            @RequestParam String term,
            @RequestParam String academicYear) {
        return ResponseEntity.ok(ApiResponse.ok(
                termGradeService.getClassStats(schoolId, classId, subjectName, term, academicYear)));
    }
}
