package com.srms.api.modules.assessment.service;

import com.srms.api.exception.ResourceNotFoundException;
import com.srms.api.modules.academic.entity.SchoolClass;
import com.srms.api.modules.academic.repository.SchoolClassRepository;
import com.srms.api.modules.assessment.dto.AnalysisResponse;
import com.srms.api.modules.assessment.dto.SubjectBreakdown;
import com.srms.api.modules.assessment.entity.Assessment;
import com.srms.api.modules.assessment.entity.PublishedTermGrade;
import com.srms.api.modules.assessment.repository.PublishedTermGradeRepository;
import com.srms.api.modules.school.entity.School;
import com.srms.api.modules.school.repository.SchoolRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ResultsAnalysisService {
    private static final int DEFAULT_PASS_MARK = 40;

    private final PublishedTermGradeRepository publishedRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final SchoolRepository schoolRepository;

    @Transactional(readOnly = true)
    public AnalysisResponse byClass(String schoolId, String classId, String term, String academicYear,
                                     Assessment.ReportingPeriod period) {
        SchoolClass schoolClass = schoolClassRepository.findByIdAndSchoolId(classId, schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found"));
        List<PublishedTermGrade> grades = publishedRepository
                .findBySchoolIdAndClassIdAndTermAndAcademicYearAndReportingPeriod(
                        schoolId, classId, term, academicYear, period);
        return aggregate(schoolId, "CLASS", schoolClass.getName(), grades);
    }

    @Transactional(readOnly = true)
    public AnalysisResponse byGrade(String schoolId, int grade, String term, String academicYear,
                                     Assessment.ReportingPeriod period) {
        List<String> classIds = schoolClassRepository.findBySchoolIdAndGrade(schoolId, grade).stream()
                .map(SchoolClass::getId).toList();
        List<PublishedTermGrade> grades = classIds.isEmpty() ? List.of() : publishedRepository
                .findBySchoolIdAndClassIdInAndTermAndAcademicYearAndReportingPeriod(
                        schoolId, classIds, term, academicYear, period);
        return aggregate(schoolId, "GRADE", "Grade " + grade, grades);
    }

    @Transactional(readOnly = true)
    public AnalysisResponse bySubject(String schoolId, String subjectName, String term, String academicYear,
                                       Assessment.ReportingPeriod period) {
        List<PublishedTermGrade> grades = publishedRepository
                .findBySchoolIdAndSubjectNameAndTermAndAcademicYearAndReportingPeriod(
                        schoolId, subjectName, term, academicYear, period);
        return aggregate(schoolId, "SUBJECT", subjectName, grades);
    }

    @Transactional(readOnly = true)
    public AnalysisResponse bySchool(String schoolId, String term, String academicYear,
                                      Assessment.ReportingPeriod period) {
        List<PublishedTermGrade> grades = publishedRepository
                .findBySchoolIdAndTermAndAcademicYearAndReportingPeriod(schoolId, term, academicYear, period);
        String schoolName = schoolRepository.findById(schoolId).map(School::getName).orElse("School");
        return aggregate(schoolId, "SCHOOL", schoolName, grades);
    }

    private AnalysisResponse aggregate(String schoolId, String scope, String scopeLabel,
                                        List<PublishedTermGrade> grades) {
        int passMark = resolvePassMark(schoolId);
        if (grades.isEmpty()) {
            return AnalysisResponse.builder()
                    .scope(scope).scopeLabel(scopeLabel).studentCount(0)
                    .average(null).passRate(null).passMarkUsed(passMark)
                    .distribution(Map.of()).bySubject(List.of()).build();
        }
        double average = grades.stream().mapToDouble(PublishedTermGrade::getWeightedTotal).average().orElse(0);
        long passed = grades.stream().filter(g -> g.getWeightedTotal() >= passMark).count();
        double passRate = (passed * 100.0) / grades.size();
        Map<String, Long> distribution = grades.stream()
                .collect(Collectors.groupingBy(PublishedTermGrade::getLetterGrade, LinkedHashMap::new, Collectors.counting()));
        List<SubjectBreakdown> bySubject = grades.stream()
                .collect(Collectors.groupingBy(PublishedTermGrade::getSubjectName))
                .entrySet().stream()
                .map(entry -> buildSubjectBreakdown(entry.getKey(), entry.getValue(), passMark))
                .sorted(Comparator.comparing(SubjectBreakdown::getSubjectName))
                .toList();
        return AnalysisResponse.builder()
                .scope(scope).scopeLabel(scopeLabel).studentCount(grades.size())
                .average(round1(average)).passRate(round1(passRate)).passMarkUsed(passMark)
                .distribution(distribution).bySubject(bySubject).build();
    }

    private SubjectBreakdown buildSubjectBreakdown(String subjectName, List<PublishedTermGrade> grades, int passMark) {
        double average = grades.stream().mapToDouble(PublishedTermGrade::getWeightedTotal).average().orElse(0);
        long passed = grades.stream().filter(g -> g.getWeightedTotal() >= passMark).count();
        double passRate = (passed * 100.0) / grades.size();
        return SubjectBreakdown.builder()
                .subjectName(subjectName).studentCount(grades.size())
                .average(round1(average)).passRate(round1(passRate)).build();
    }

    private int resolvePassMark(String schoolId) {
        return schoolRepository.findById(schoolId).map(School::getPassMark)
                .filter(Objects::nonNull).orElse(DEFAULT_PASS_MARK);
    }

    private static Double round1(double value) {
        return Math.round(value * 10) / 10.0;
    }
}
