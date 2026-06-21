package com.srms.api.modules.activities.repository;

import com.srms.api.modules.activities.entity.ActivityEnrolment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ActivityEnrolmentRepository extends JpaRepository<ActivityEnrolment, String> {
    List<ActivityEnrolment> findBySchoolId(String schoolId);
    List<ActivityEnrolment> findBySchoolIdAndActivityId(String schoolId, String activityId);
    List<ActivityEnrolment> findBySchoolIdAndStudentId(String schoolId, String studentId);
}
