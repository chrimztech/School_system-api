package com.srms.api.modules.audit.repository;

import com.srms.api.modules.audit.entity.AuditEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AuditEventRepository extends JpaRepository<AuditEvent, String> {
    List<AuditEvent> findBySchoolIdOrderByCreatedAtDesc(String schoolId);
}
