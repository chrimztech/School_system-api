package com.srms.api.modules.strategic.repository;

import com.srms.api.modules.strategic.entity.StrategicGoal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface StrategicGoalRepository extends JpaRepository<StrategicGoal, String> {
    List<StrategicGoal> findBySchoolIdOrderByCreatedAtAsc(String schoolId);
}
