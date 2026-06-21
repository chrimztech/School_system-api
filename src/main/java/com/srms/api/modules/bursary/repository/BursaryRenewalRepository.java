package com.srms.api.modules.bursary.repository;

import com.srms.api.modules.bursary.entity.BursaryRenewal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BursaryRenewalRepository extends JpaRepository<BursaryRenewal, String> {
    List<BursaryRenewal> findBySchoolIdOrderByReviewDateAscCreatedAtDesc(String schoolId);
}
