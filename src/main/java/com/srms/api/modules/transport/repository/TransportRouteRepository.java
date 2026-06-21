package com.srms.api.modules.transport.repository;

import com.srms.api.modules.transport.entity.TransportRoute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TransportRouteRepository extends JpaRepository<TransportRoute, String> {
    List<TransportRoute> findBySchoolId(String schoolId);
}
