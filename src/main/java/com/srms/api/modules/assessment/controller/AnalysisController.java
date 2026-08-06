package com.srms.api.modules.assessment.controller;

import com.srms.api.common.ApiResponse;
import com.srms.api.exception.ForbiddenException;
import com.srms.api.modules.assessment.dto.AnalysisResponse;
import com.srms.api.modules.assessment.entity.Assessment;
import com.srms.api.modules.assessment.service.ResultsAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/schools/{schoolId}/results-analysis")
@RequiredArgsConstructor
public class AnalysisController {
    private final ResultsAnalysisService resultsAnalysisService;

    @GetMapping("/by-class")
    public ResponseEntity<ApiResponse<AnalysisResponse>> byClass(
            @PathVariable String schoolId, @RequestParam String classId,
            @RequestParam String term, @RequestParam String academicYear,
            @RequestParam Assessment.ReportingPeriod reportingPeriod, Authentication auth) {
        assertActorSchool(schoolId, auth);
        return ResponseEntity.ok(ApiResponse.ok(
                resultsAnalysisService.byClass(schoolId, classId, term, academicYear, reportingPeriod)));
    }

    @GetMapping("/by-grade")
    public ResponseEntity<ApiResponse<AnalysisResponse>> byGrade(
            @PathVariable String schoolId, @RequestParam int grade,
            @RequestParam String term, @RequestParam String academicYear,
            @RequestParam Assessment.ReportingPeriod reportingPeriod, Authentication auth) {
        assertActorSchool(schoolId, auth);
        return ResponseEntity.ok(ApiResponse.ok(
                resultsAnalysisService.byGrade(schoolId, grade, term, academicYear, reportingPeriod)));
    }

    @GetMapping("/by-subject")
    public ResponseEntity<ApiResponse<AnalysisResponse>> bySubject(
            @PathVariable String schoolId, @RequestParam String subjectName,
            @RequestParam String term, @RequestParam String academicYear,
            @RequestParam Assessment.ReportingPeriod reportingPeriod, Authentication auth) {
        assertActorSchool(schoolId, auth);
        return ResponseEntity.ok(ApiResponse.ok(
                resultsAnalysisService.bySubject(schoolId, subjectName, term, academicYear, reportingPeriod)));
    }

    @GetMapping("/by-school")
    public ResponseEntity<ApiResponse<AnalysisResponse>> bySchool(
            @PathVariable String schoolId, @RequestParam String term,
            @RequestParam String academicYear, @RequestParam Assessment.ReportingPeriod reportingPeriod,
            Authentication auth) {
        assertActorSchool(schoolId, auth);
        return ResponseEntity.ok(ApiResponse.ok(
                resultsAnalysisService.bySchool(schoolId, term, academicYear, reportingPeriod)));
    }

    private static String roleOf(Authentication auth) {
        return auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).findFirst()
                .map(authority -> authority.replaceFirst("^ROLE_", "")).orElse("");
    }

    private void assertActorSchool(String schoolId, Authentication auth) {
        if ("SUPER_ADMIN".equalsIgnoreCase(roleOf(auth))) return;
        String actorSchool = auth.getCredentials() == null ? null : auth.getCredentials().toString();
        if (!schoolId.equals(actorSchool)) {
            throw new ForbiddenException("You cannot access another school's results analysis");
        }
    }
}
