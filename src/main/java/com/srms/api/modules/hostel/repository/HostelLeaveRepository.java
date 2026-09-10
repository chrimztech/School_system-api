package com.srms.api.modules.hostel.repository;

import com.srms.api.modules.hostel.entity.HostelLeave;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface HostelLeaveRepository extends JpaRepository<HostelLeave, String> {
    List<HostelLeave> findBySchoolIdOrderByCreatedAtDesc(String schoolId);
    List<HostelLeave> findBySchoolIdAndStudentId(String schoolId, String studentId);
}
