package com.srms.api.modules.development.repository;

import com.srms.api.modules.development.entity.TrainingRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TrainingRepository extends JpaRepository<TrainingRecord, String> {
    List<TrainingRecord> findBySchoolIdOrderByStartDateDesc(String schoolId);
}
