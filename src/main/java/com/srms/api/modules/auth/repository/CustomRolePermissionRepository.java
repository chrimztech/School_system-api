package com.srms.api.modules.auth.repository;

import com.srms.api.modules.auth.entity.CustomRolePermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface CustomRolePermissionRepository extends JpaRepository<CustomRolePermission, String> {
    List<CustomRolePermission> findBySchoolIdAndRoleName(String schoolId, String roleName);

    Optional<CustomRolePermission> findBySchoolIdAndRoleNameAndModule(String schoolId, String roleName, String module);

    @Transactional
    void deleteBySchoolIdAndRoleName(String schoolId, String roleName);
}
