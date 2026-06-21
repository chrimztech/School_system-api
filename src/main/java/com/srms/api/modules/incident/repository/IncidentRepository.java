package com.srms.api.modules.incident.repository;

import com.srms.api.modules.incident.entity.Incident;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface IncidentRepository extends JpaRepository<Incident, String> {
    List<Incident> findBySchoolIdOrderByIncidentDateDesc(String schoolId);
}
