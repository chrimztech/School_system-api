package com.srms.api.modules.assessment.repository;

import com.srms.api.modules.assessment.entity.Assessment;
import com.srms.api.modules.assessment.entity.PublishedTermGrade;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PublishedTermGradeRepository extends JpaRepository<PublishedTermGrade, String> {
    List<PublishedTermGrade> findBySchoolIdAndStudentIdAndAcademicYearAndReportingPeriod(
            String schoolId, String studentId, String academicYear, Assessment.ReportingPeriod reportingPeriod);

    List<PublishedTermGrade> findBySchoolIdAndClassIdAndSubjectNameAndTermAndAcademicYearAndReportingPeriod(
            String schoolId, String classId, String subjectName, String term, String academicYear,
            Assessment.ReportingPeriod reportingPeriod);

    Optional<PublishedTermGrade> findBySchoolIdAndStudentIdAndSubjectNameAndTermAndAcademicYearAndReportingPeriod(
            String schoolId, String studentId, String subjectName, String term, String academicYear,
            Assessment.ReportingPeriod reportingPeriod);
}
