package com.srms.api.modules.discipline.repository;

import com.srms.api.modules.discipline.entity.DisciplineCase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DisciplineRepository extends JpaRepository<DisciplineCase, String> {
    List<DisciplineCase> findBySchoolIdOrderByIncidentDateDesc(String schoolId);
    List<DisciplineCase> findBySchoolIdAndStudentId(String schoolId, String studentId);
    List<DisciplineCase> findBySchoolIdAndStatus(String schoolId, String status);
    long countBySchoolIdAndStatus(String schoolId, String status);
}
