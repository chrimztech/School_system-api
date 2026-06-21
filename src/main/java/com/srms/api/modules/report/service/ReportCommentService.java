package com.srms.api.modules.report.service;

import com.srms.api.modules.report.entity.ReportComment;
import com.srms.api.modules.report.repository.ReportCommentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service @RequiredArgsConstructor
public class ReportCommentService {
    private final ReportCommentRepository repo;

    public Optional<ReportComment> find(String schoolId, String studentId, String term, String academicYear) {
        return repo.findBySchoolIdAndStudentIdAndTermAndAcademicYear(schoolId, studentId, term, academicYear);
    }

    public ReportComment upsert(String schoolId, String studentId, String term, String academicYear,
                                String teacherComment, String headComment) {
        ReportComment rc = repo.findBySchoolIdAndStudentIdAndTermAndAcademicYear(schoolId, studentId, term, academicYear)
                .orElse(ReportComment.builder().schoolId(schoolId).studentId(studentId).term(term).academicYear(academicYear).build());
        rc.setTeacherComment(teacherComment);
        rc.setHeadComment(headComment);
        return repo.save(rc);
    }
}
