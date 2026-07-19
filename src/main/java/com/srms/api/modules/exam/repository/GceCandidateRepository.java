package com.srms.api.modules.exam.repository;

import com.srms.api.modules.exam.entity.GceCandidate;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface GceCandidateRepository extends JpaRepository<GceCandidate, String> {
    List<GceCandidate> findBySchoolIdOrderByLastNameAsc(String schoolId);
    Optional<GceCandidate> findByIdAndSchoolId(String id, String schoolId);
    List<GceCandidate> findBySchoolIdAndIdIn(String schoolId, List<String> ids);
    boolean existsByExamNumber(String examNumber);
}
