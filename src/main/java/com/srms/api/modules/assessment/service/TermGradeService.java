package com.srms.api.modules.assessment.service;

import com.srms.api.exception.ResourceNotFoundException;
import com.srms.api.modules.academic.entity.ClassEnrolment;
import com.srms.api.modules.academic.entity.SchoolClass;
import com.srms.api.modules.academic.repository.ClassEnrolmentRepository;
import com.srms.api.modules.academic.repository.SchoolClassRepository;
import com.srms.api.modules.assessment.entity.Assessment;
import com.srms.api.modules.assessment.entity.AssessmentResult;
import com.srms.api.modules.assessment.entity.GradeWeightConfig;
import com.srms.api.modules.assessment.entity.TermGrade;
import com.srms.api.modules.assessment.repository.AssessmentRepository;
import com.srms.api.modules.assessment.repository.ResultRepository;
import com.srms.api.modules.assessment.repository.TermGradeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TermGradeService {
    private static final Set<Assessment.AssessmentType> CA_TYPES = EnumSet.of(
            Assessment.AssessmentType.cat, Assessment.AssessmentType.project,
            Assessment.AssessmentType.homework, Assessment.AssessmentType.quiz,
            Assessment.AssessmentType.practical);

    private final SchoolClassRepository schoolClassRepository;
    private final ClassEnrolmentRepository classEnrolmentRepository;
    private final AssessmentRepository assessmentRepository;
    private final ResultRepository resultRepository;
    private final TermGradeRepository termGradeRepository;
    private final GradeWeightConfigService gradeWeightConfigService;

    public String resolveClassId(String schoolId, String classIdOrName) {
        Optional<SchoolClass> byId = schoolClassRepository.findByIdAndSchoolId(classIdOrName, schoolId);
        if (byId.isPresent()) return byId.get().getId();
        SchoolClass byName = schoolClassRepository.findBySchoolIdAndName(schoolId, classIdOrName)
                .orElseThrow(() -> new ResourceNotFoundException("SchoolClass", classIdOrName));
        return byName.getId();
    }

    public List<TermGrade> compute(String schoolId, String classIdOrName, String subjectName, String term, String academicYear) {
        String classId = resolveClassId(schoolId, classIdOrName);
        GradeWeightConfig weights = gradeWeightConfigService.get(schoolId);

        List<ClassEnrolment> enrolments = classEnrolmentRepository.findByClassIdAndSchoolId(classId, schoolId).stream()
                .filter(e -> "ACTIVE".equalsIgnoreCase(e.getStatus()))
                .filter(e -> e.getAcademicYear() == null || e.getAcademicYear().equals(academicYear))
                .collect(Collectors.toList());

        List<Assessment> assessments = assessmentRepository.findBySchoolIdAndSubjectNameAndTermAndAcademicYear(
                schoolId, subjectName, term, academicYear);
        Map<String, Assessment> assessmentById = assessments.stream()
                .collect(Collectors.toMap(Assessment::getId, a -> a));

        List<String> assessmentIds = new ArrayList<>(assessmentById.keySet());
        Map<String, List<AssessmentResult>> resultsByStudent = resultRepository.findByAssessmentIdIn(assessmentIds).stream()
                .collect(Collectors.groupingBy(AssessmentResult::getStudentId));

        List<TermGrade> saved = new ArrayList<>();
        for (ClassEnrolment enrolment : enrolments) {
            List<AssessmentResult> studentResults = resultsByStudent.getOrDefault(enrolment.getStudentId(), List.of());
            Double caPercent = categoryPercent(studentResults, assessmentById, CA_TYPES);
            Double midtermPercent = categoryPercent(studentResults, assessmentById, EnumSet.of(Assessment.AssessmentType.midterm));
            Double examPercent = categoryPercent(studentResults, assessmentById, EnumSet.of(Assessment.AssessmentType.exam));

            if (caPercent == null && midtermPercent == null && examPercent == null) continue;

            double weightSum = 0;
            double weightedSum = 0;
            if (caPercent != null) { weightedSum += caPercent * weights.getCaWeight(); weightSum += weights.getCaWeight(); }
            if (midtermPercent != null) { weightedSum += midtermPercent * weights.getMidtermWeight(); weightSum += weights.getMidtermWeight(); }
            if (examPercent != null) { weightedSum += examPercent * weights.getExamWeight(); weightSum += weights.getExamWeight(); }
            double weightedTotal = weightSum > 0 ? weightedSum / weightSum : 0;
            boolean complete = caPercent != null && midtermPercent != null && examPercent != null;

            TermGrade grade = termGradeRepository
                    .findBySchoolIdAndStudentIdAndSubjectNameAndTermAndAcademicYear(schoolId, enrolment.getStudentId(), subjectName, term, academicYear)
                    .orElse(TermGrade.builder()
                            .schoolId(schoolId).studentId(enrolment.getStudentId())
                            .subjectName(subjectName).term(term).academicYear(academicYear)
                            .build());
            grade.setClassId(classId);
            grade.setStudentName(enrolment.getStudentName());
            grade.setCaPercent(caPercent);
            grade.setMidtermPercent(midtermPercent);
            grade.setExamPercent(examPercent);
            grade.setWeightedTotal(weightedTotal);
            grade.setLetterGrade(GradeThresholds.letterGrade(weightedTotal));
            grade.setComplete(complete);
            saved.add(termGradeRepository.save(grade));
        }
        return saved;
    }

    private Double categoryPercent(List<AssessmentResult> studentResults, Map<String, Assessment> assessmentById,
                                    Set<Assessment.AssessmentType> types) {
        double weightedSum = 0;
        double weightSum = 0;
        for (AssessmentResult result : studentResults) {
            if (result.isAbsent()) continue;
            Assessment assessment = assessmentById.get(result.getAssessmentId());
            if (assessment == null || assessment.getType() == null || !types.contains(assessment.getType())) continue;
            if (assessment.getMaxScore() <= 0) continue;
            double pct = (result.getScore() / assessment.getMaxScore()) * 100;
            double weight = assessment.getWeight() > 0 ? assessment.getWeight() : 1;
            weightedSum += pct * weight;
            weightSum += weight;
        }
        return weightSum > 0 ? weightedSum / weightSum : null;
    }

    public TermGrade publish(String id, String schoolId) {
        TermGrade grade = termGradeRepository.findByIdAndSchoolId(id, schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("TermGrade", id));
        grade.setPublished(true);
        return termGradeRepository.save(grade);
    }

    public List<TermGrade> getHistory(String schoolId, String studentId, String academicYear, boolean includeUnpublished) {
        return includeUnpublished
                ? termGradeRepository.findBySchoolIdAndStudentIdAndAcademicYear(schoolId, studentId, academicYear)
                : termGradeRepository.findBySchoolIdAndStudentIdAndAcademicYearAndPublishedTrue(schoolId, studentId, academicYear);
    }

    public Map<String, Object> getClassStats(String schoolId, String classIdOrName, String subjectName, String term, String academicYear) {
        String classId = resolveClassId(schoolId, classIdOrName);
        List<TermGrade> grades = termGradeRepository.findBySchoolIdAndClassIdAndSubjectNameAndTermAndAcademicYear(
                schoolId, classId, subjectName, term, academicYear);

        Map<String, Object> stats = new LinkedHashMap<>();
        if (grades.isEmpty()) {
            stats.put("average", null);
            stats.put("distribution", Map.of());
            return stats;
        }
        double average = grades.stream().mapToDouble(TermGrade::getWeightedTotal).average().orElse(0);
        Map<String, Long> distribution = grades.stream()
                .collect(Collectors.groupingBy(TermGrade::getLetterGrade, LinkedHashMap::new, Collectors.counting()));
        stats.put("average", Math.round(average * 10) / 10.0);
        stats.put("distribution", distribution);
        return stats;
    }
}
