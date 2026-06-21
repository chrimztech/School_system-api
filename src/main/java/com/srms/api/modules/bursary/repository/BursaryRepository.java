package com.srms.api.modules.bursary.repository;

import com.srms.api.modules.bursary.entity.BursaryAward;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BursaryRepository extends JpaRepository<BursaryAward, String> {
    List<BursaryAward> findBySchoolIdOrderByCreatedAtDesc(String schoolId);
}
