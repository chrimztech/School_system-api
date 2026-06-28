package com.srms.api.modules.assessment.service;
import com.srms.api.exception.ResourceNotFoundException;
import com.srms.api.modules.assessment.entity.Assessment;
import com.srms.api.modules.assessment.entity.AssessmentResult;
import com.srms.api.modules.assessment.repository.AssessmentRepository;
import com.srms.api.modules.assessment.repository.ResultRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
@Service @RequiredArgsConstructor
public class AssessmentService {
    private final AssessmentRepository assessmentRepository;
    private final ResultRepository resultRepository;
    public List<Assessment> findAll(String schoolId) { return assessmentRepository.findBySchoolIdOrderByDateDesc(schoolId); }
    public Assessment findById(String id, String schoolId) { return assessmentRepository.findByIdAndSchoolId(id, schoolId).orElseThrow(() -> new ResourceNotFoundException("Assessment", id)); }
    public Assessment create(String schoolId, Assessment dto) { dto.setSchoolId(schoolId); return assessmentRepository.save(dto); }
    public Assessment update(String id, String schoolId, Assessment dto) { Assessment a = findById(id, schoolId); if (dto.getTitle() != null) a.setTitle(dto.getTitle()); if (dto.getMaxScore() > 0) a.setMaxScore(dto.getMaxScore()); if (dto.getWeight() > 0) a.setWeight(dto.getWeight()); a.setPublished(dto.isPublished()); return assessmentRepository.save(a); }
    public List<AssessmentResult> getResults(String assessmentId) { return resultRepository.findByAssessmentId(assessmentId); }
    public AssessmentResult saveResult(AssessmentResult result) { return resultRepository.save(result); }
    public List<AssessmentResult> saveResultsBulk(String assessmentId, String schoolId, List<AssessmentResult> results) {
        results.forEach(r -> { r.setAssessmentId(assessmentId); r.setSchoolId(schoolId); });
        resultRepository.deleteByAssessmentId(assessmentId);
        return resultRepository.saveAll(results);
    }
    public List<AssessmentResult> getStudentResults(String schoolId, String studentId) { return resultRepository.findBySchoolIdAndStudentId(schoolId, studentId); }

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
