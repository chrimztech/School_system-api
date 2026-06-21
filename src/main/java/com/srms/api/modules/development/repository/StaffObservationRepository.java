package com.srms.api.modules.development.repository;

import com.srms.api.modules.development.entity.StaffObservation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StaffObservationRepository extends JpaRepository<StaffObservation, String> {
    List<StaffObservation> findBySchoolIdOrderByDateDescCreatedAtDesc(String schoolId);
}
