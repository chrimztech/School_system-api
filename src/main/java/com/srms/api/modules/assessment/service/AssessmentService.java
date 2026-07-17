package com.srms.api.modules.assessment.service;
import com.srms.api.exception.ResourceNotFoundException;
import com.srms.api.modules.assessment.entity.Assessment;
import com.srms.api.modules.assessment.entity.AssessmentResult;
import com.srms.api.modules.assessment.repository.AssessmentRepository;
import com.srms.api.modules.assessment.repository.ResultRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
@Service @RequiredArgsConstructor @Transactional @Slf4j
public class AssessmentService {
    private final AssessmentRepository assessmentRepository;
    private final ResultRepository resultRepository;
    private final TermGradeService termGradeService;
    public List<Assessment> findAll(String schoolId) { return assessmentRepository.findBySchoolIdOrderByDateDesc(schoolId); }
    public List<Assessment> findAll(String schoolId, String term, String academicYear) {
        if (term == null && academicYear == null) return findAll(schoolId);
        return assessmentRepository.findBySchoolIdAndTermAndAcademicYear(schoolId, term, academicYear);
    }
    public Assessment findById(String id, String schoolId) { return assessmentRepository.findByIdAndSchoolId(id, schoolId).orElseThrow(() -> new ResourceNotFoundException("Assessment", id)); }
    public Assessment create(String schoolId, Assessment dto) { dto.setSchoolId(schoolId); return assessmentRepository.save(dto); }
    public Assessment update(String id, String schoolId, Assessment dto) { Assessment a = findById(id, schoolId); if (dto.getTitle() != null) a.setTitle(dto.getTitle()); if (dto.getMaxScore() > 0) a.setMaxScore(dto.getMaxScore()); if (dto.getWeight() > 0) a.setWeight(dto.getWeight()); if (dto.getSubjectName() != null) a.setSubjectName(dto.getSubjectName()); if (dto.getSubjectId() != null) a.setSubjectId(dto.getSubjectId()); if (dto.getTerm() != null) a.setTerm(dto.getTerm()); if (dto.getAcademicYear() != null) a.setAcademicYear(dto.getAcademicYear()); a.setPublished(dto.isPublished()); return assessmentRepository.save(a); }
    public List<AssessmentResult> getResults(String assessmentId) { return resultRepository.findByAssessmentId(assessmentId); }
    public AssessmentResult saveResult(AssessmentResult result) { return resultRepository.save(result); }
    public List<AssessmentResult> saveResultsBulk(String assessmentId, String schoolId, List<AssessmentResult> results) {
        results.forEach(r -> { r.setAssessmentId(assessmentId); r.setSchoolId(schoolId); });
        resultRepository.deleteByAssessmentId(assessmentId);
        List<AssessmentResult> saved = resultRepository.saveAll(results);
        assessmentRepository.findByIdAndSchoolId(assessmentId, schoolId).ifPresent(a -> {
            a.setSubmitted(saved.size());
            a.setTotal(saved.size());
            assessmentRepository.save(a);
            // Keep report cards populated automatically: recompute (but don't publish)
            // this class/subject/term's TermGrade rows every time marks are saved, so
            // staff always see current standing without a separate manual step. Parents
            // still only see it once a teacher explicitly publishes (unchanged).
            if (a.getClassId() != null && a.getSubjectName() != null && a.getTerm() != null && a.getAcademicYear() != null) {
                try {
                    termGradeService.compute(schoolId, a.getClassId(), a.getSubjectName(), a.getTerm(), a.getAcademicYear());
                } catch (Exception e) {
                    log.warn("Term grade auto-compute failed for assessment {}: {}", assessmentId, e.getMessage());
                }
            }
        });
        return saved;
    }
    public List<AssessmentResult> getStudentResults(String schoolId, String studentId) { return resultRepository.findBySchoolIdAndStudentId(schoolId, studentId); }
    public Page<AssessmentResult> getStudentResultsPaged(String schoolId, String studentId, Pageable pageable) { return resultRepository.findBySchoolIdAndStudentId(schoolId, studentId, pageable); }

    public List<Map<String, Object>> getEnrichedStudentResults(String schoolId, String studentId) {
        List<AssessmentResult> results = resultRepository.findBySchoolIdAndStudentId(schoolId, studentId);
        return results.stream().map(r -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", r.getId());
            m.put("assessmentId", r.getAssessmentId());
            m.put("score", r.getScore());
            m.put("grade", r.getGrade());
            m.put("remarks", r.getRemarks());
            m.put("absent", r.isAbsent());
            assessmentRepository.findById(r.getAssessmentId()).ifPresent(a -> {
                m.put("title", a.getTitle());
                m.put("subjectName", a.getSubjectName());
                m.put("type", a.getType() != null ? a.getType().name() : null);
                m.put("maxScore", a.getMaxScore());
                m.put("date", a.getDate() != null ? a.getDate().toString() : null);
                m.put("weight", a.getWeight());
                m.put("published", a.isPublished());
            });
            return m;
        }).collect(Collectors.toList());
    }
}
