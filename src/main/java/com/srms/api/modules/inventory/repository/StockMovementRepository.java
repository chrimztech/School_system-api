package com.srms.api.modules.inventory.repository;

import com.srms.api.modules.inventory.entity.StockMovement;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface StockMovementRepository extends JpaRepository<StockMovement, String> {
    List<StockMovement> findBySchoolIdOrderByMovementDateDesc(String schoolId);
    List<StockMovement> findBySchoolIdAndItemId(String schoolId, String itemId);
}
