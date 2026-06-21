package com.srms.api.modules.transport.repository;

import com.srms.api.modules.transport.entity.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, String> {
    List<Vehicle> findBySchoolId(String schoolId);
    List<Vehicle> findBySchoolIdAndStatus(String schoolId, Vehicle.Status status);
}
