package com.srms.api.modules.auth.service;

import com.srms.api.exception.BusinessException;
import com.srms.api.modules.auth.entity.CustomRole;
import com.srms.api.modules.auth.entity.CustomRolePermission;
import com.srms.api.modules.auth.repository.CustomRolePermissionRepository;
import com.srms.api.modules.auth.repository.CustomRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RoleService {

    private final CustomRoleRepository customRoleRepository;
    private final CustomRolePermissionRepository permissionRepository;

    public List<CustomRole> listCustomRoles(String schoolId) {
        return customRoleRepository.findBySchoolIdAndActiveTrue(schoolId);
    }

    public CustomRole createCustomRole(String schoolId, CustomRole dto) {
        if (dto.getName() == null || dto.getName().isBlank()) {
            throw new BusinessException("Role name is required");
        }
        if (customRoleRepository.existsBySchoolIdAndName(schoolId, dto.getName().trim())) {
            throw new BusinessException("A role with this name already exists");
        }
        CustomRole role = CustomRole.builder()
                .schoolId(schoolId)
                .name(dto.getName().trim())
                .description(dto.getDescription())
                .active(true)
                .build();
        return customRoleRepository.save(role);
    }

    public CustomRole updateCustomRole(String id, String schoolId, CustomRole dto) {
        CustomRole role = customRoleRepository.findByIdAndSchoolId(id, schoolId)
                .orElseThrow(() -> new BusinessException("Role not found"));
        if (dto.getName() == null || dto.getName().isBlank()) {
            throw new BusinessException("Role name is required");
        }
        String trimmed = dto.getName().trim();
        if (!trimmed.equals(role.getName()) &&
                customRoleRepository.existsBySchoolIdAndNameAndIdNot(schoolId, trimmed, id)) {
            throw new BusinessException("A role with this name already exists");
        }
        role.setName(trimmed);
        role.setDescription(dto.getDescription());
        return customRoleRepository.save(role);
    }

    public void deleteCustomRole(String id, String schoolId) {
        CustomRole role = customRoleRepository.findByIdAndSchoolId(id, schoolId)
                .orElseThrow(() -> new BusinessException("Role not found"));
        role.setActive(false);
        customRoleRepository.save(role);
    }

    public List<CustomRolePermission> getPermissions(String schoolId, String roleName) {
        return permissionRepository.findBySchoolIdAndRoleName(schoolId, roleName);
    }

    @Transactional
    public List<CustomRolePermission> savePermissions(
            String schoolId, String roleName, List<Map<String, String>> permissions) {
        permissionRepository.deleteBySchoolIdAndRoleName(schoolId, roleName);
        List<CustomRolePermission> entities = permissions.stream()
                .filter(p -> p.get("module") != null && p.get("access") != null)
                .map(p -> CustomRolePermission.builder()
                        .schoolId(schoolId)
                        .roleName(roleName)
                        .module(p.get("module"))
                        .access(p.get("access"))
                        .build())
                .toList();
        return permissionRepository.saveAll(entities);
    }
}
