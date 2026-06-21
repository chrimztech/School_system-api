package com.srms.api.modules.assessment.repository;
import com.srms.api.modules.assessment.entity.AssessmentResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
@Repository
public interface ResultRepository extends JpaRepository<AssessmentResult, String> {
    List<AssessmentResult> findByAssessmentId(String assessmentId);
    List<AssessmentResult> findBySchoolIdAndStudentId(String schoolId, String studentId);
}
