package com.srms.api.modules.report.repository;

import com.srms.api.modules.report.entity.ReportComment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ReportCommentRepository extends JpaRepository<ReportComment, String> {
    Optional<ReportComment> findBySchoolIdAndStudentIdAndTermAndAcademicYear(
            String schoolId, String studentId, String term, String academicYear);
}
