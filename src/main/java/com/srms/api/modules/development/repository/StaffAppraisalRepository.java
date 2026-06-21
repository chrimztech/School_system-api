package com.srms.api.modules.development.repository;

import com.srms.api.modules.development.entity.StaffAppraisal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StaffAppraisalRepository extends JpaRepository<StaffAppraisal, String> {
    List<StaffAppraisal> findBySchoolIdOrderByCreatedAtDesc(String schoolId);
}
