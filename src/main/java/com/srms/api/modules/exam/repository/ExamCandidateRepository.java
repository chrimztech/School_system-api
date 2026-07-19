package com.srms.api.modules.exam.repository;

import com.srms.api.modules.exam.entity.ExamCandidate;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ExamCandidateRepository extends JpaRepository<ExamCandidate, String> {
    List<ExamCandidate> findByExamPaperIdAndSchoolId(String examPaperId, String schoolId);
    long countByExamPaperId(String examPaperId);
    boolean existsByExamPaperIdAndStudentId(String examPaperId, String studentId);
    boolean existsByExamPaperIdAndGceCandidateId(String examPaperId, String gceCandidateId);
    Optional<ExamCandidate> findByIdAndSchoolId(String id, String schoolId);
}
