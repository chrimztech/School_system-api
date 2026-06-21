package com.srms.api.modules.fee.repository;

import com.srms.api.modules.fee.entity.FeeLevy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FeeLevyRepository extends JpaRepository<FeeLevy, String> {
    List<FeeLevy> findBySchoolIdOrderByCreatedAtDesc(String schoolId);
}
