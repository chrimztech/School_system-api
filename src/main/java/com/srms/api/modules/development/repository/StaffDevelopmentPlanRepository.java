package com.srms.api.modules.development.repository;

import com.srms.api.modules.development.entity.StaffDevelopmentPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StaffDevelopmentPlanRepository extends JpaRepository<StaffDevelopmentPlan, String> {
    List<StaffDevelopmentPlan> findBySchoolIdOrderByCreatedAtDesc(String schoolId);
}
