package com.srms.api.modules.assessment.repository;
import com.srms.api.modules.assessment.entity.Assessment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
@Repository
public interface AssessmentRepository extends JpaRepository<Assessment, String> {
    List<Assessment> findBySchoolIdOrderByDateDesc(String schoolId);
    Optional<Assessment> findByIdAndSchoolId(String id, String schoolId);
    List<Assessment> findBySchoolIdAndClassId(String schoolId, String classId);
    List<Assessment> findBySchoolIdAndClassIdAndTermAndAcademicYear(String schoolId, String classId, String term, String academicYear);
    List<Assessment> findBySchoolIdAndTermAndAcademicYear(String schoolId, String term, String academicYear);
    List<Assessment> findBySchoolIdAndSubjectNameAndTermAndAcademicYear(String schoolId, String subjectName, String term, String academicYear);
}
