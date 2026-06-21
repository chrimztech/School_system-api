package com.srms.api.modules.integration.repository;

import com.srms.api.modules.integration.entity.IntegrationConnection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface IntegrationConnectionRepository extends JpaRepository<IntegrationConnection, String> {
    List<IntegrationConnection> findBySchoolIdOrderByCreatedAtDesc(String schoolId);
    Optional<IntegrationConnection> findBySchoolIdAndCode(String schoolId, String code);
}
