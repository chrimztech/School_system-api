package com.srms.api.modules.integration.repository;

import com.srms.api.modules.integration.entity.IntegrationConfig;
import com.srms.api.modules.integration.entity.PowerBiReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PowerBiReportRepository extends JpaRepository<PowerBiReport, String> {
    List<PowerBiReport> findByScopeTypeAndSchoolIdOrderByDisplayNameAsc(IntegrationConfig.ScopeType scopeType, String schoolId);
    Optional<PowerBiReport> findByIdAndScopeTypeAndSchoolId(String id, IntegrationConfig.ScopeType scopeType, String schoolId);
}
