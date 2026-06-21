package com.srms.api.modules.health.repository;

import com.srms.api.modules.health.entity.HealthVisit;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface HealthVisitRepository extends JpaRepository<HealthVisit, String> {
    List<HealthVisit> findBySchoolIdOrderByVisitDateDesc(String schoolId);
    List<HealthVisit> findBySchoolIdAndStudentId(String schoolId, String studentId);
}
