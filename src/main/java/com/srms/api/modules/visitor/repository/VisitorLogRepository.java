package com.srms.api.modules.visitor.repository;

import com.srms.api.modules.visitor.entity.VisitorLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface VisitorLogRepository extends JpaRepository<VisitorLog, String> {
    List<VisitorLog> findBySchoolIdOrderByCheckInTimeDesc(String schoolId);
    List<VisitorLog> findBySchoolIdAndStatus(String schoolId, String status);
}
