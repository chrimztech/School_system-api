package com.srms.api.modules.hostel.repository;

import com.srms.api.modules.hostel.entity.HostelAllocation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface HostelAllocationRepository extends JpaRepository<HostelAllocation, String> {
    List<HostelAllocation> findBySchoolId(String schoolId);
    List<HostelAllocation> findBySchoolIdAndStatus(String schoolId, String status);
    List<HostelAllocation> findBySchoolIdAndStudentId(String schoolId, String studentId);
}
