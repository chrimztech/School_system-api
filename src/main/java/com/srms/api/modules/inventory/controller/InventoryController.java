package com.srms.api.modules.inventory.controller;

import com.srms.api.common.ApiResponse;
import com.srms.api.modules.inventory.entity.InventoryItem;
import com.srms.api.modules.inventory.entity.StockMovement;
import com.srms.api.modules.inventory.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/schools/{schoolId}/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<InventoryItem>>> getAll(@PathVariable String schoolId) {
        return ResponseEntity.ok(ApiResponse.ok(inventoryService.getAllItems(schoolId)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<InventoryItem>> create(@PathVariable String schoolId, @RequestBody InventoryItem item) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(inventoryService.createItem(schoolId, item)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<InventoryItem>> update(@PathVariable String schoolId, @PathVariable String id, @RequestBody InventoryItem item) {
        return ResponseEntity.ok(ApiResponse.ok(inventoryService.updateItem(schoolId, id, item)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String schoolId, @PathVariable String id) {
        inventoryService.deleteItem(schoolId, id);
        return ResponseEntity.ok(ApiResponse.ok("Item deleted", null));
    }

    @GetMapping("/movements")
    public ResponseEntity<ApiResponse<List<StockMovement>>> getMovements(@PathVariable String schoolId) {
        return ResponseEntity.ok(ApiResponse.ok(inventoryService.getMovements(schoolId)));
    }

    @PostMapping("/movements")
    public ResponseEntity<ApiResponse<StockMovement>> recordMovement(@PathVariable String schoolId, @RequestBody StockMovement movement) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(inventoryService.recordMovement(schoolId, movement)));
    }
}
