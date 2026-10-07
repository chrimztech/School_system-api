package com.srms.api.modules.insights.service;

import com.srms.api.exception.BusinessException;
import com.srms.api.exception.ResourceNotFoundException;
import com.srms.api.modules.assessment.entity.PublishedTermGrade;
import com.srms.api.modules.assessment.repository.PublishedTermGradeRepository;
import com.srms.api.modules.insights.dto.PerformanceInsightView;
import com.srms.api.modules.insights.dto.PerformanceInsightView.SubjectPerformance;
import com.srms.api.modules.integration.client.AnthropicClient;
import com.srms.api.modules.school.entity.School;
import com.srms.api.modules.school.repository.SchoolRepository;
import com.srms.api.modules.student.entity.Student;
import com.srms.api.modules.student.repository.StudentRepository;
import com.srms.api.modules.tuition.entity.TuitionCenter;
import com.srms.api.modules.tuition.service.TuitionCenterService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Turns a pupil's already-published results into a plain-language reading of where they need
 * extra support, and matches each weak subject to nearby, well-rated tuition centres. Everything
 * here is deterministic (real averages, real proximity/rating ranking) — the only place AI
 * enters is writing the parent-facing paragraph in generateNarrative, which restates these exact
 * facts rather than inventing its own assessment of the pupil.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PerformanceInsightService {
    /** Below half marks, regardless of each school's own letter-grade scale — simple and the
     * same for every parent to understand, rather than relying on a school-customised grading
     * band whose wording ("Basic", "Satisfactory", ...) isn't consistent across schools. */
    private static final double WEAK_THRESHOLD = 50.0;
    private static final int CENTRES_PER_SUBJECT = 3;
    private static final String NARRATIVE_SYSTEM_PROMPT = """
            You write short, warm, practical notes for parents at Zambian schools about their
            child's academic performance. Be encouraging and specific, 120-180 words. Mention the
            subjects that need extra support and, when tuition centres are listed, name them with
            their district and say briefly why each is a sensible choice (closest first, and
            mention the rating only if one is given). Do not invent facts beyond what is given to
            you, and do not suggest a centre that isn't in the list.
            """;

    private final PublishedTermGradeRepository publishedRepository;
    private final StudentRepository studentRepository;
    private final SchoolRepository schoolRepository;
    private final TuitionCenterService tuitionCenterService;
    private final AnthropicClient anthropicClient;

    public PerformanceInsightView analyze(String schoolId, String studentId, String requestedYear) {
        Student student = studentRepository.findByIdAndSchoolId(studentId, schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("Student", studentId));
        List<PublishedTermGrade> all = publishedRepository.findBySchoolIdAndStudentId(schoolId, studentId);

        String year = requestedYear != null && !requestedYear.isBlank() ? requestedYear
                : all.stream().map(PublishedTermGrade::getAcademicYear).filter(y -> y != null)
                        .max(Comparator.naturalOrder()).orElse(null);

        List<PublishedTermGrade> yearGrades = year == null ? List.of()
                : all.stream().filter(g -> year.equals(g.getAcademicYear())).toList();

        List<SubjectPerformance> subjects = yearGrades.stream()
                .collect(Collectors.groupingBy(PublishedTermGrade::getSubjectName))
                .entrySet().stream()
                .map(e -> toSubjectPerformance(e.getKey(), e.getValue()))
                .sorted(Comparator.comparingDouble(SubjectPerformance::average))
                .toList();

        List<SubjectPerformance> weakSubjects = subjects.stream()
                .filter(s -> s.average() < WEAK_THRESHOLD)
                .toList();

        Double overall = subjects.isEmpty() ? null
                : subjects.stream().mapToDouble(SubjectPerformance::average).average().orElse(0);

        Map<String, List<TuitionCenter>> recommended = weakSubjects.isEmpty() ? Map.of() : recommendFor(schoolId, weakSubjects);

        String studentName = (student.getFirstName() + " " + student.getLastName()).trim();
        return new PerformanceInsightView(studentId, studentName, year, overall, subjects, weakSubjects, recommended);
    }

    @Transactional
    public String generateNarrative(String schoolId, String studentId, String requestedYear) {
        PerformanceInsightView insight = analyze(schoolId, studentId, requestedYear);
        if (insight.weakSubjects().isEmpty()) {
            throw new BusinessException("No subjects currently need extra support — nothing to write a note about.");
        }
        return anthropicClient.complete(schoolId, NARRATIVE_SYSTEM_PROMPT, narrativePrompt(insight), 500);
    }

    private Map<String, List<TuitionCenter>> recommendFor(String schoolId, List<SubjectPerformance> weakSubjects) {
        School school = schoolRepository.findById(schoolId).orElse(null);
        String district = school == null ? null : school.getDistrict();
        String province = school == null ? null : school.getProvince();
        List<String> names = weakSubjects.stream().map(SubjectPerformance::subjectName).toList();
        return tuitionCenterService.recommend(names, district, province, CENTRES_PER_SUBJECT);
    }

    private static SubjectPerformance toSubjectPerformance(String subjectName, List<PublishedTermGrade> grades) {
        double avg = grades.stream().mapToDouble(PublishedTermGrade::getWeightedTotal).average().orElse(0);
        String grade = grades.stream()
                .max(Comparator.comparing(PublishedTermGrade::getTerm))
                .map(PublishedTermGrade::getLetterGrade).orElse(null);
        return new SubjectPerformance(subjectName, Math.round(avg * 10) / 10.0, grade);
    }

    private static String narrativePrompt(PerformanceInsightView insight) {
        StringBuilder sb = new StringBuilder();
        sb.append("Pupil: ").append(insight.studentName()).append("\n");
        sb.append("Academic year: ").append(insight.academicYear()).append("\n");
        sb.append("Overall average: ").append(insight.overallAverage() == null ? "n/a" : insight.overallAverage()).append("%\n\n");
        sb.append("Subjects needing extra support:\n");
        for (SubjectPerformance s : insight.weakSubjects()) {
            sb.append("- ").append(s.subjectName()).append(": ").append(s.average()).append("%");
            if (s.letterGrade() != null) sb.append(" (grade ").append(s.letterGrade()).append(")");
            sb.append("\n");
            List<TuitionCenter> centres = insight.recommendedCentres().getOrDefault(s.subjectName(), List.of());
            if (centres.isEmpty()) {
                sb.append("  No tuition centres currently listed for this subject.\n");
            } else {
                for (TuitionCenter c : centres) {
                    sb.append("  * ").append(c.getName()).append(" (").append(c.getDistrict()).append(")");
                    if (c.getRating() != null) sb.append(", rating ").append(c.getRating()).append("/5");
                    sb.append("\n");
                }
            }
        }
        return sb.toString();
    }
}
