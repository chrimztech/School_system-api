package com.srms.api.modules.school.repository;

import com.srms.api.modules.school.entity.School;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface SchoolRepository extends JpaRepository<School, String> {
    Optional<School> findByShortCode(String shortCode);
    List<School> findByActiveTrue();
    List<School> findBySubscriptionStatus(School.SubscriptionStatus status);

    @Query("SELECT s.shortCode FROM School s WHERE s.id = :id")
    Optional<String> findShortCodeById(String id);
}
