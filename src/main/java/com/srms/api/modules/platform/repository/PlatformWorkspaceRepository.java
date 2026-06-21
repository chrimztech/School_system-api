package com.srms.api.modules.platform.repository;

import com.srms.api.modules.platform.entity.PlatformWorkspace;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PlatformWorkspaceRepository extends JpaRepository<PlatformWorkspace, String> {
    Optional<PlatformWorkspace> findByWorkspaceKey(String workspaceKey);
}
