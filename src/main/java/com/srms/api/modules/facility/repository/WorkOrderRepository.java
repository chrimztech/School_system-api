package com.srms.api.modules.facility.repository;

import com.srms.api.modules.facility.entity.WorkOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface WorkOrderRepository extends JpaRepository<WorkOrder, String> {
    List<WorkOrder> findBySchoolIdOrderByCreatedAtDesc(String schoolId);
}
