package com.srms.api.modules.integration.repository;

import com.srms.api.modules.integration.entity.PlatformIntegrationConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlatformIntegrationConfigRepository extends JpaRepository<PlatformIntegrationConfig, String> {
    Optional<PlatformIntegrationConfig> findByProvider(String provider);
    List<PlatformIntegrationConfig> findAllByOrderByProviderAsc();
}
