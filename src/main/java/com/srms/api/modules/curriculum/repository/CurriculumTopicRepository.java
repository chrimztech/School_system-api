package com.srms.api.modules.curriculum.repository;

import com.srms.api.modules.curriculum.entity.CurriculumTopic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CurriculumTopicRepository extends JpaRepository<CurriculumTopic, String> {
    List<CurriculumTopic> findBySchoolIdAndSubjectNameAndGradeAndTermOrderBySequenceAsc(String schoolId, String subjectName, Integer grade, String term);
    void deleteBySchoolIdAndSubjectNameAndGradeAndTerm(String schoolId, String subjectName, Integer grade, String term);
}
