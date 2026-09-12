package com.srms.api.modules.integration.repository;

import com.srms.api.modules.integration.entity.IntegrationConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IntegrationConfigRepository extends JpaRepository<IntegrationConfig, String> {
    Optional<IntegrationConfig> findByScopeTypeAndSchoolIdAndProviderCode(
            IntegrationConfig.ScopeType scopeType, String schoolId, String providerCode);

    List<IntegrationConfig> findByScopeTypeAndSchoolIdOrderByDisplayNameAsc(
            IntegrationConfig.ScopeType scopeType, String schoolId);
}
