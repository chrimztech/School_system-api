package com.srms.api.modules.canteen.repository;

import com.srms.api.modules.canteen.entity.CanteenOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CanteenOrderRepository extends JpaRepository<CanteenOrder, String> {
    List<CanteenOrder> findBySchoolIdOrderByOrderDateDesc(String schoolId);
    List<CanteenOrder> findBySchoolIdAndStatus(String schoolId, String status);
}
