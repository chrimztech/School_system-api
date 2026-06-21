package com.srms.api.modules.assessment.service;
import com.srms.api.exception.ResourceNotFoundException;
import com.srms.api.modules.assessment.entity.Assessment;
import com.srms.api.modules.assessment.entity.AssessmentResult;
import com.srms.api.modules.assessment.repository.AssessmentRepository;
import com.srms.api.modules.assessment.repository.ResultRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
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
    public List<AssessmentResult> getStudentResults(String schoolId, String studentId) { return resultRepository.findBySchoolIdAndStudentId(schoolId, studentId); }
}
