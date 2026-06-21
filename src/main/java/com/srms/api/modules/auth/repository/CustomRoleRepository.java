package com.srms.api.modules.auth.repository;

import com.srms.api.modules.auth.entity.CustomRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CustomRoleRepository extends JpaRepository<CustomRole, String> {
    List<CustomRole> findBySchoolIdAndActiveTrue(String schoolId);
    Optional<CustomRole> findByIdAndSchoolId(String id, String schoolId);
    boolean existsBySchoolIdAndName(String schoolId, String name);
    boolean existsBySchoolIdAndNameAndIdNot(String schoolId, String name, String id);
}
