package com.srms.api.modules.bursary.repository;

import com.srms.api.modules.bursary.entity.BursaryApplication;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BursaryApplicationRepository extends JpaRepository<BursaryApplication, String> {
    List<BursaryApplication> findBySchoolIdOrderByCreatedAtDesc(String schoolId);
}
