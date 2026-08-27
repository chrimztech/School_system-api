package com.srms.api.modules.assessment.service;

import com.srms.api.exception.ResourceNotFoundException;
import com.srms.api.modules.academic.entity.ClassEnrolment;
import com.srms.api.modules.academic.entity.SchoolClass;
import com.srms.api.modules.academic.repository.ClassEnrolmentRepository;
import com.srms.api.modules.academic.repository.SchoolClassRepository;
import com.srms.api.modules.assessment.dto.GradingBandDto;
import com.srms.api.modules.assessment.entity.Assessment;
import com.srms.api.modules.assessment.entity.AssessmentResult;
import com.srms.api.modules.assessment.entity.GradeWeightConfig;
import com.srms.api.modules.assessment.entity.PublishedTermGrade;
import com.srms.api.modules.assessment.entity.TermGrade;
import com.srms.api.modules.assessment.repository.AssessmentRepository;
import com.srms.api.modules.assessment.repository.PublishedTermGradeRepository;
import com.srms.api.modules.assessment.repository.ResultRepository;
import com.srms.api.modules.assessment.repository.TermGradeRepository;
import com.srms.api.modules.school.entity.School;
import com.srms.api.modules.school.repository.SchoolRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
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
    private final PublishedTermGradeRepository publishedRepository;
    private final GradeWeightConfigService gradeWeightConfigService;
    private final GradingScaleService gradingScaleService;
    private final SchoolRepository schoolRepository;

    public String resolveClassId(String schoolId, String classIdOrName) {
        return resolveClass(schoolId, classIdOrName).getId();
    }

    private SchoolClass resolveClass(String schoolId, String classIdOrName) {
        Optional<SchoolClass> byId = schoolClassRepository.findByIdAndSchoolId(classIdOrName, schoolId);
        if (byId.isPresent()) return byId.get();
        return schoolClassRepository.findBySchoolIdAndName(schoolId, classIdOrName)
                .orElseThrow(() -> new ResourceNotFoundException("SchoolClass", classIdOrName));
    }

    public List<TermGrade> compute(String schoolId, String classIdOrName, String subjectName,
                                   String term, String academicYear) {
        return computeInternal(schoolId, classIdOrName, subjectName, term, academicYear, null, false);
    }

    public List<TermGrade> computeForPublication(String schoolId, String classIdOrName, String subjectName,
                                                 String term, String academicYear,
                                                 Assessment.ReportingPeriod period) {
        return computeInternal(schoolId, classIdOrName, subjectName, term, academicYear, period, true);
    }

    private List<TermGrade> computeInternal(String schoolId, String classIdOrName, String subjectName,
                                            String term, String academicYear,
                                            Assessment.ReportingPeriod period, boolean publicationReadyOnly) {
        SchoolClass schoolClass = resolveClass(schoolId, classIdOrName);
        String classId = schoolClass.getId();
        GradeWeightConfig weights = gradeWeightConfigService.get(schoolId);
        String publicationMode = schoolRepository.findById(schoolId)
                .map(School::getResultPublicationMode).orElse("SEPARATE");
        List<GradingBandDto> gradingBands = gradingScaleService.getBands(schoolId);

        List<ClassEnrolment> enrolments = classEnrolmentRepository.findByClassIdAndSchoolId(classId, schoolId).stream()
                .filter(e -> "ACTIVE".equalsIgnoreCase(e.getStatus()))
                .filter(e -> e.getAcademicYear() == null || e.getAcademicYear().equals(academicYear))
                .toList();

        List<Assessment> assessments = assessmentRepository.findBySchoolIdAndSubjectNameAndTermAndAcademicYear(
                        schoolId, subjectName, term, academicYear).stream()
                .filter(a -> classMatches(a, schoolClass, classIdOrName))
                .filter(a -> includedInPeriod(a, period, publicationMode))
                .filter(a -> !publicationReadyOnly || a.getWorkflowStatus() == Assessment.WorkflowStatus.VERIFIED
                        || a.getWorkflowStatus() == Assessment.WorkflowStatus.PUBLISHED)
                .toList();
        Map<String, Assessment> assessmentById = assessments.stream()
                .collect(Collectors.toMap(Assessment::getId, Function.identity()));

        List<AssessmentResult> allResults = assessmentById.isEmpty()
                ? List.of()
                : resultRepository.findByAssessmentIdIn(new ArrayList<>(assessmentById.keySet()));
        Map<String, List<AssessmentResult>> resultsByStudent = allResults.stream()
                .collect(Collectors.groupingBy(AssessmentResult::getStudentId));

        // recomputeProvisional (AssessmentService) re-runs this on every single result save,
        // so a teacher entering one class's marks one student at a time used to cost 2 DB
        // round trips per remaining student, per keystroke — an O(class size) tax repeated
        // O(class size) times. One batched fetch + one batched save turns the whole thing
        // into a fixed, small number of round trips regardless of class size.
        Map<String, TermGrade> existingByStudent = termGradeRepository
                .findBySchoolIdAndClassIdAndSubjectNameAndTermAndAcademicYear(schoolId, classId, subjectName, term, academicYear)
                .stream()
                .collect(Collectors.toMap(TermGrade::getStudentId, Function.identity(), (a, b) -> a));

        List<TermGrade> saved = new ArrayList<>();
        for (ClassEnrolment enrolment : enrolments) {
            List<AssessmentResult> studentResults = resultsByStudent.getOrDefault(enrolment.getStudentId(), List.of());
            Double caPercent = categoryPercent(studentResults, assessmentById, CA_TYPES);
            Double midtermPercent = categoryPercent(studentResults, assessmentById,
                    EnumSet.of(Assessment.AssessmentType.midterm));
            Double examPercent = categoryPercent(studentResults, assessmentById,
                    EnumSet.of(Assessment.AssessmentType.exam));

            if (caPercent == null && midtermPercent == null && examPercent == null) continue;

            double weightSum = 0;
            double weightedSum = 0;
            if (caPercent != null) { weightedSum += caPercent * weights.getCaWeight(); weightSum += weights.getCaWeight(); }
            if (midtermPercent != null) { weightedSum += midtermPercent * weights.getMidtermWeight(); weightSum += weights.getMidtermWeight(); }
            if (examPercent != null) { weightedSum += examPercent * weights.getExamWeight(); weightSum += weights.getExamWeight(); }
            double weightedTotal = weightSum > 0 ? weightedSum / weightSum : 0;
            GradingBandDto band = gradingScaleService.evaluate(gradingBands, weightedTotal);

            TermGrade grade = existingByStudent.getOrDefault(enrolment.getStudentId(),
                    TermGrade.builder()
                            .schoolId(schoolId).studentId(enrolment.getStudentId())
                            .subjectName(subjectName).term(term).academicYear(academicYear)
                            .build());
            grade.setClassId(classId);
            grade.setStudentName(enrolment.getStudentName());
            grade.setCaPercent(caPercent);
            grade.setMidtermPercent(midtermPercent);
            grade.setExamPercent(examPercent);
            grade.setWeightedTotal(weightedTotal);
            grade.setLetterGrade(band.getGrade());
            grade.setGradeDescription(band.getDescription());
            grade.setGradePoints(band.getPoints());
            grade.setComplete(publicationReadyOnly || (caPercent != null && midtermPercent != null && examPercent != null));
            saved.add(grade);
        }
        return termGradeRepository.saveAll(saved);
    }

    private boolean classMatches(Assessment assessment, SchoolClass schoolClass, String requested) {
        String stored = assessment.getClassId();
        return stored != null && (stored.equals(requested) || stored.equals(schoolClass.getId())
                || stored.equalsIgnoreCase(schoolClass.getName()));
    }

    private boolean includedInPeriod(Assessment assessment, Assessment.ReportingPeriod period, String publicationMode) {
        if (period == null) return true;
        Assessment.ReportingPeriod effective = effectivePeriod(assessment, publicationMode);
        if (period == Assessment.ReportingPeriod.END_TERM) {
            return effective == Assessment.ReportingPeriod.MIDTERM || effective == Assessment.ReportingPeriod.END_TERM;
        }
        return effective == period;
    }

    public Assessment.ReportingPeriod effectivePeriod(Assessment assessment, String publicationMode) {
        if ("COMBINED".equalsIgnoreCase(publicationMode)) return Assessment.ReportingPeriod.COMBINED;
        if (assessment.getReportingPeriod() == Assessment.ReportingPeriod.MIDTERM
                || assessment.getReportingPeriod() == Assessment.ReportingPeriod.END_TERM) {
            return assessment.getReportingPeriod();
        }
        return assessment.getType() == Assessment.AssessmentType.midterm
                ? Assessment.ReportingPeriod.MIDTERM : Assessment.ReportingPeriod.END_TERM;
    }

    private Double categoryPercent(List<AssessmentResult> studentResults, Map<String, Assessment> assessmentById,
                                   Set<Assessment.AssessmentType> types) {
        double weightedSum = 0;
        double weightSum = 0;
        for (AssessmentResult result : studentResults) {
            Assessment assessment = assessmentById.get(result.getAssessmentId());
            if (assessment == null || assessment.getType() == null || !types.contains(assessment.getType())) continue;
            if (assessment.getMaxScore() <= 0) continue;
            if (!result.isAbsent() && result.getScore() == null) continue;
            double pct = result.isAbsent() ? 0 : (result.getScore() / assessment.getMaxScore()) * 100;
            double weight = assessment.getWeight() > 0 ? assessment.getWeight() : 1;
            weightedSum += pct * weight;
            weightSum += weight;
        }
        return weightSum > 0 ? weightedSum / weightSum : null;
    }

    public List<PublishedTermGrade> publishSnapshots(String schoolId, String classIdOrName, String term,
                                                     String academicYear, Assessment.ReportingPeriod period,
                                                     Set<String> subjects, String publishedBy) {
        // One batched fetch of every subject's existing snapshot for this class/term/period,
        // instead of a query per (subject, student) pair — a 10-subject, 40-pupil publish used
        // to cost 400 round trips; this brings it down to one lookup query plus one batched save.
        String resolvedClassId = resolveClass(schoolId, classIdOrName).getId();
        Map<String, PublishedTermGrade> existingByKey = publishedRepository
                .findBySchoolIdAndClassIdAndTermAndAcademicYearAndReportingPeriod(schoolId, resolvedClassId, term, academicYear, period)
                .stream()
                .collect(Collectors.toMap(g -> g.getStudentId() + "|" + g.getSubjectName(), Function.identity(), (a, b) -> a));

        List<PublishedTermGrade> toSave = new ArrayList<>();
        for (String subject : subjects) {
            List<TermGrade> grades = computeForPublication(
                    schoolId, classIdOrName, subject, term, academicYear, period);
            for (TermGrade grade : grades) {
                PublishedTermGrade snapshot = existingByKey.getOrDefault(
                        grade.getStudentId() + "|" + grade.getSubjectName(),
                        PublishedTermGrade.builder()
                                .schoolId(schoolId).studentId(grade.getStudentId())
                                .subjectName(grade.getSubjectName()).term(term).academicYear(academicYear)
                                .reportingPeriod(period).build());
                snapshot.setStudentName(grade.getStudentName());
                snapshot.setClassId(grade.getClassId());
                snapshot.setCaPercent(grade.getCaPercent());
                snapshot.setMidtermPercent(grade.getMidtermPercent());
                snapshot.setExamPercent(grade.getExamPercent());
                snapshot.setWeightedTotal(grade.getWeightedTotal());
                snapshot.setLetterGrade(grade.getLetterGrade());
                snapshot.setGradeDescription(grade.getGradeDescription());
                snapshot.setGradePoints(grade.getGradePoints());
                snapshot.setPublishedBy(publishedBy);
                snapshot.setPublishedAt(LocalDateTime.now());
                toSave.add(snapshot);
            }
        }
        return publishedRepository.saveAll(toSave);
    }

    @Transactional(readOnly = true)
    public List<PublishedTermGrade> getPublishedHistory(String schoolId, String studentId, String academicYear,
                                                        Assessment.ReportingPeriod period) {
        List<PublishedTermGrade> grades = (academicYear == null || period == null)
                ? publishedRepository.findBySchoolIdAndStudentId(schoolId, studentId)
                : publishedRepository.findBySchoolIdAndStudentIdAndAcademicYearAndReportingPeriod(
                        schoolId, studentId, academicYear, period);
        // Records published before gradeDescription/gradePoints existed on this entity carry nulls;
        // fill them in from the current grading scale so old report cards don't show a blank Remarks column.
        if (grades.stream().anyMatch(g -> g.getGradeDescription() == null || g.getGradeDescription().isBlank())) {
            List<GradingBandDto> bands = gradingScaleService.getBands(schoolId);
            for (PublishedTermGrade grade : grades) {
                if (grade.getGradeDescription() != null && !grade.getGradeDescription().isBlank()) continue;
                GradingBandDto band = gradingScaleService.findByGrade(bands, grade.getLetterGrade());
                if (band != null) { grade.setGradeDescription(band.getDescription()); grade.setGradePoints(band.getPoints()); }
            }
        }
        return grades;
    }

    public List<TermGrade> getHistory(String schoolId, String studentId, String academicYear, boolean includeUnpublished) {
        List<TermGrade> grades = includeUnpublished
                ? termGradeRepository.findBySchoolIdAndStudentIdAndAcademicYear(schoolId, studentId, academicYear)
                : termGradeRepository.findBySchoolIdAndStudentIdAndAcademicYearAndPublishedTrue(schoolId, studentId, academicYear);
        if (grades.stream().anyMatch(g -> g.getGradeDescription() == null || g.getGradeDescription().isBlank())) {
            List<GradingBandDto> bands = gradingScaleService.getBands(schoolId);
            for (TermGrade grade : grades) {
                if (grade.getGradeDescription() != null && !grade.getGradeDescription().isBlank()) continue;
                GradingBandDto band = gradingScaleService.findByGrade(bands, grade.getLetterGrade());
                if (band != null) { grade.setGradeDescription(band.getDescription()); grade.setGradePoints(band.getPoints()); }
            }
        }
        return grades;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getPublishedClassStats(String schoolId, String classIdOrName, String subjectName,
                                                      String term, String academicYear,
                                                      Assessment.ReportingPeriod period) {
        String classId = resolveClassId(schoolId, classIdOrName);
        List<PublishedTermGrade> grades = publishedRepository
                .findBySchoolIdAndClassIdAndSubjectNameAndTermAndAcademicYearAndReportingPeriod(
                        schoolId, classId, subjectName, term, academicYear, period);
        return stats(grades.stream().map(PublishedTermGrade::getWeightedTotal).toList(),
                grades.stream().map(PublishedTermGrade::getLetterGrade).toList());
    }

    public Map<String, Object> getClassStats(String schoolId, String classIdOrName, String subjectName,
                                             String term, String academicYear) {
        String classId = resolveClassId(schoolId, classIdOrName);
        List<TermGrade> grades = termGradeRepository
                .findBySchoolIdAndClassIdAndSubjectNameAndTermAndAcademicYear(
                        schoolId, classId, subjectName, term, academicYear);
        return stats(grades.stream().map(TermGrade::getWeightedTotal).toList(),
                grades.stream().map(TermGrade::getLetterGrade).toList());
    }

    private Map<String, Object> stats(List<Double> totals, List<String> letters) {
        Map<String, Object> stats = new LinkedHashMap<>();
        if (totals.isEmpty()) {
            stats.put("average", null);
            stats.put("distribution", Map.of());
            return stats;
        }
        double average = totals.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        Map<String, Long> distribution = letters.stream()
                .collect(Collectors.groupingBy(Function.identity(), LinkedHashMap::new, Collectors.counting()));
        stats.put("average", Math.round(average * 10) / 10.0);
        stats.put("distribution", distribution);
        return stats;
    }
}
