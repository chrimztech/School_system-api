package com.srms.api.modules.risk.repository;

import com.srms.api.modules.risk.entity.RiskEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface RiskRepository extends JpaRepository<RiskEntry, String> {
    List<RiskEntry> findBySchoolIdOrderByCreatedAtDesc(String schoolId);
}
