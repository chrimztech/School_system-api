package com.srms.api.modules.insights.dto;

import com.srms.api.modules.tuition.entity.TuitionCenter;

import java.util.List;
import java.util.Map;

public record PerformanceInsightView(
        String studentId,
        String studentName,
        String academicYear,
        Double overallAverage,
        List<SubjectPerformance> subjects,
        List<SubjectPerformance> weakSubjects,
        Map<String, List<TuitionCenter>> recommendedCentres) {

    public record SubjectPerformance(String subjectName, double average, String letterGrade) {}
}
