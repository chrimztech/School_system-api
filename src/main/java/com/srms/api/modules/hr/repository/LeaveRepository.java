package com.srms.api.modules.hr.repository;

import com.srms.api.modules.hr.entity.LeaveRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface LeaveRepository extends JpaRepository<LeaveRequest, String> {
    List<LeaveRequest> findBySchoolId(String schoolId);
    List<LeaveRequest> findBySchoolIdAndStatus(String schoolId, LeaveRequest.LeaveStatus status);
    List<LeaveRequest> findBySchoolIdAndStaffId(String schoolId, String staffId);
}
