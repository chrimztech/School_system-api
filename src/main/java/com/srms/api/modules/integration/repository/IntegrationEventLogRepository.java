package com.srms.api.modules.integration.repository;

import com.srms.api.modules.integration.entity.IntegrationConfig;
import com.srms.api.modules.integration.entity.IntegrationEventLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IntegrationEventLogRepository extends JpaRepository<IntegrationEventLog, String> {
    Page<IntegrationEventLog> findByScopeTypeAndSchoolIdAndProviderCodeOrderByCreatedAtDesc(
            IntegrationConfig.ScopeType scopeType, String schoolId, String providerCode, Pageable pageable);
}
