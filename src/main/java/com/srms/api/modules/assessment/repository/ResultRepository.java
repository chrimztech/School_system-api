package com.srms.api.modules.assessment.repository;
import com.srms.api.modules.assessment.entity.AssessmentResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
@Repository
public interface ResultRepository extends JpaRepository<AssessmentResult, String> {
    List<AssessmentResult> findByAssessmentId(String assessmentId);
    List<AssessmentResult> findByAssessmentIdIn(List<String> assessmentIds);
    List<AssessmentResult> findBySchoolIdAndStudentId(String schoolId, String studentId);
    Page<AssessmentResult> findBySchoolIdAndStudentId(String schoolId, String studentId, Pageable pageable);
    Optional<AssessmentResult> findByAssessmentIdAndStudentId(String assessmentId, String studentId);
    void deleteByAssessmentId(String assessmentId);
}
