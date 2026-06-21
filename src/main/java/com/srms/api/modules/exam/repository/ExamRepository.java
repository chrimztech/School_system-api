package com.srms.api.modules.exam.repository;

import com.srms.api.modules.exam.entity.ExamPaper;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ExamRepository extends JpaRepository<ExamPaper, String> {
    List<ExamPaper> findBySchoolIdOrderByExamDateAsc(String schoolId);
}
