package com.srms.api.modules.health.repository;

import com.srms.api.modules.health.entity.HealthRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface HealthRecordRepository extends JpaRepository<HealthRecord, String> {
    List<HealthRecord> findBySchoolId(String schoolId);
    Optional<HealthRecord> findBySchoolIdAndStudentId(String schoolId, String studentId);
}
