package com.srms.api.modules.strategic.repository;

import com.srms.api.modules.strategic.entity.StrategicReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface StrategicReviewRepository extends JpaRepository<StrategicReview, String> {
    List<StrategicReview> findBySchoolIdOrderByReviewDateDesc(String schoolId);
}
