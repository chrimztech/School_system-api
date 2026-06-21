package com.srms.api.modules.activities.repository;

import com.srms.api.modules.activities.entity.Activity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ActivityRepository extends JpaRepository<Activity, String> {
    List<Activity> findBySchoolId(String schoolId);
    List<Activity> findBySchoolIdAndStatus(String schoolId, String status);
}
