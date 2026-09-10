package com.srms.api.modules.assessment.repository;
import com.srms.api.modules.assessment.entity.TermGrade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
@Repository
public interface TermGradeRepository extends JpaRepository<TermGrade, String> {
    List<TermGrade> findBySchoolIdAndStudentId(String schoolId, String studentId);
    List<TermGrade> findBySchoolIdAndStudentIdAndAcademicYear(String schoolId, String studentId, String academicYear);
    List<TermGrade> findBySchoolIdAndStudentIdAndAcademicYearAndPublishedTrue(String schoolId, String studentId, String academicYear);
    List<TermGrade> findBySchoolIdAndClassIdAndSubjectNameAndTermAndAcademicYear(String schoolId, String classId, String subjectName, String term, String academicYear);
    Optional<TermGrade> findBySchoolIdAndStudentIdAndSubjectNameAndTermAndAcademicYear(String schoolId, String studentId, String subjectName, String term, String academicYear);
    Optional<TermGrade> findByIdAndSchoolId(String id, String schoolId);
}
