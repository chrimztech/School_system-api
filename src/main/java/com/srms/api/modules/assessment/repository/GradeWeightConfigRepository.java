package com.srms.api.modules.assessment.repository;
import com.srms.api.modules.assessment.entity.GradeWeightConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
@Repository
public interface GradeWeightConfigRepository extends JpaRepository<GradeWeightConfig, String> {
    Optional<GradeWeightConfig> findBySchoolId(String schoolId);
}
