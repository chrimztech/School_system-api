package com.srms.api.modules.academic.repository;

import com.srms.api.modules.academic.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface DepartmentRepository extends JpaRepository<Department, String> {
    List<Department>     findBySchoolIdAndActiveTrue(String schoolId);
    Optional<Department> findByIdAndSchoolId(String id, String schoolId);
    boolean existsBySchoolIdAndName(String schoolId, String name);
    List<Department> findBySchoolIdAndHeadTeacherId(String schoolId, String headTeacherId);
}
