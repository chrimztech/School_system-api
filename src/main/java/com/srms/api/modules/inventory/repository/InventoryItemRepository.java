package com.srms.api.modules.inventory.repository;

import com.srms.api.modules.inventory.entity.InventoryItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface InventoryItemRepository extends JpaRepository<InventoryItem, String> {
    List<InventoryItem> findBySchoolId(String schoolId);
    List<InventoryItem> findBySchoolIdAndStatus(String schoolId, String status);
    List<InventoryItem> findBySchoolIdAndCategory(String schoolId, String category);
}
