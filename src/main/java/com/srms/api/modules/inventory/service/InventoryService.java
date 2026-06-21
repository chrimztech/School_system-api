package com.srms.api.modules.inventory.service;

import com.srms.api.exception.ResourceNotFoundException;
import com.srms.api.modules.inventory.entity.InventoryItem;
import com.srms.api.modules.inventory.entity.StockMovement;
import com.srms.api.modules.inventory.repository.InventoryItemRepository;
import com.srms.api.modules.inventory.repository.StockMovementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class InventoryService {

    private final InventoryItemRepository itemRepository;
    private final StockMovementRepository movementRepository;

    public List<InventoryItem> getAllItems(String schoolId) {
        return itemRepository.findBySchoolId(schoolId);
    }

    public InventoryItem getItem(String schoolId, String id) {
        return itemRepository.findById(id)
                .filter(i -> i.getSchoolId().equals(schoolId))
                .orElseThrow(() -> new ResourceNotFoundException("InventoryItem", id));
    }

    public InventoryItem createItem(String schoolId, InventoryItem item) {
        item.setSchoolId(schoolId);
        item.setStatus(computeStatus(item.getQuantityInStock(), item.getReorderLevel()));
        long count = itemRepository.findBySchoolId(schoolId).size();
        item.setItemCode(schoolId.toUpperCase() + "-INV-" + String.format("%03d", count + 1));
        return itemRepository.save(item);
    }

    public InventoryItem updateItem(String schoolId, String id, InventoryItem updated) {
        InventoryItem item = getItem(schoolId, id);
        item.setName(updated.getName());
        item.setCategory(updated.getCategory());
        item.setUnit(updated.getUnit());
        item.setQuantityInStock(updated.getQuantityInStock());
        item.setReorderLevel(updated.getReorderLevel());
        item.setUnitCost(updated.getUnitCost());
        item.setLocation(updated.getLocation());
        item.setStatus(computeStatus(updated.getQuantityInStock(), updated.getReorderLevel()));
        return itemRepository.save(item);
    }

    public void deleteItem(String schoolId, String id) {
        InventoryItem item = getItem(schoolId, id);
        itemRepository.delete(item);
    }

    public StockMovement recordMovement(String schoolId, StockMovement movement) {
        movement.setSchoolId(schoolId);
        if (movement.getMovementDate() == null) movement.setMovementDate(LocalDate.now());
        InventoryItem item = itemRepository.findById(movement.getItemId()).orElse(null);
        if (item != null) {
            if ("IN".equals(movement.getMovementType())) {
                item.setQuantityInStock(item.getQuantityInStock() + movement.getQuantity());
                item.setLastRestockedDate(LocalDate.now());
            } else if ("OUT".equals(movement.getMovementType())) {
                item.setQuantityInStock(Math.max(0, item.getQuantityInStock() - movement.getQuantity()));
            }
            item.setStatus(computeStatus(item.getQuantityInStock(), item.getReorderLevel()));
            itemRepository.save(item);
            movement.setItemName(item.getName());
        }
        return movementRepository.save(movement);
    }

    public List<StockMovement> getMovements(String schoolId) {
        return movementRepository.findBySchoolIdOrderByMovementDateDesc(schoolId);
    }

    private String computeStatus(int qty, int reorderLevel) {
        if (qty == 0) return "OUT_OF_STOCK";
        if (qty <= reorderLevel) return "LOW_STOCK";
        return "IN_STOCK";
    }
}
