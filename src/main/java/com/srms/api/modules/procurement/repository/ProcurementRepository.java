package com.srms.api.modules.procurement.repository;

import com.srms.api.modules.procurement.entity.ProcurementRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProcurementRepository extends JpaRepository<ProcurementRequest, String> {
    List<ProcurementRequest> findBySchoolIdOrderByCreatedAtDesc(String schoolId);
}
