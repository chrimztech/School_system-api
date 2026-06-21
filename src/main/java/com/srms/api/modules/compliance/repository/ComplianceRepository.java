package com.srms.api.modules.compliance.repository;

import com.srms.api.modules.compliance.entity.ComplianceItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ComplianceRepository extends JpaRepository<ComplianceItem, String> {
    List<ComplianceItem> findBySchoolIdOrderByDueDateAsc(String schoolId);
}
