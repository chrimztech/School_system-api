package com.srms.api.modules.curriculum.repository;

import com.srms.api.modules.curriculum.entity.LessonPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LessonPlanRepository extends JpaRepository<LessonPlan, String> {
    List<LessonPlan> findBySchoolIdOrderByLessonDateDescCreatedAtDesc(String schoolId);
    List<LessonPlan> findBySchoolIdAndClassIdOrderByLessonDateDesc(String schoolId, String classId);
    Optional<LessonPlan> findByIdAndSchoolId(String id, String schoolId);
}
