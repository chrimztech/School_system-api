package com.srms.api.modules.curriculum.repository;

import com.srms.api.modules.curriculum.entity.CurriculumDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CurriculumDocumentRepository extends JpaRepository<CurriculumDocument, String> {
    List<CurriculumDocument> findBySchoolIdOrderBySubjectNameAscGradeAscVersionDesc(String schoolId);
    List<CurriculumDocument> findBySchoolIdAndSubjectNameAndGradeOrderByVersionDesc(String schoolId, String subjectName, Integer grade);
    Optional<CurriculumDocument> findByIdAndSchoolId(String id, String schoolId);
    Optional<CurriculumDocument> findFirstBySchoolIdAndSubjectNameAndGradeAndAcademicYearOrderByVersionDesc(String schoolId, String subjectName, Integer grade, String academicYear);
}
