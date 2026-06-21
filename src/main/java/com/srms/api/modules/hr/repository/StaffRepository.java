package com.srms.api.modules.hr.repository;

import com.srms.api.modules.hr.entity.StaffRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface StaffRepository extends JpaRepository<StaffRecord, String> {
    List<StaffRecord> findBySchoolId(String schoolId);
    List<StaffRecord> findBySchoolIdAndStatus(String schoolId, StaffRecord.StaffStatus status);
    Optional<StaffRecord> findBySchoolIdAndUserId(String schoolId, String userId);
}
