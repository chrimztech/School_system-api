package com.srms.api.modules.canteen.controller;

import com.srms.api.common.ApiResponse;
import com.srms.api.modules.canteen.entity.CanteenOrder;
import com.srms.api.modules.canteen.entity.DailyMenu;
import com.srms.api.modules.canteen.entity.MenuItem;
import com.srms.api.modules.canteen.service.CanteenService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController @RequestMapping("/api/schools/{schoolId}/canteen") @RequiredArgsConstructor
public class CanteenController {
    private final CanteenService canteenService;

    @GetMapping("/menu") public ResponseEntity<ApiResponse<List<MenuItem>>> getMenu(@PathVariable String schoolId) { return ResponseEntity.ok(ApiResponse.ok(canteenService.getMenu(schoolId))); }
    @PostMapping("/menu") public ResponseEntity<ApiResponse<MenuItem>> createItem(@PathVariable String schoolId, @RequestBody MenuItem item) { return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(canteenService.createMenuItem(schoolId, item))); }
    @PutMapping("/menu/{id}") public ResponseEntity<ApiResponse<MenuItem>> updateItem(@PathVariable String schoolId, @PathVariable String id, @RequestBody MenuItem item) { return ResponseEntity.ok(ApiResponse.ok(canteenService.updateMenuItem(schoolId, id, item))); }
    @DeleteMapping("/menu/{id}") public ResponseEntity<ApiResponse<Void>> deleteItem(@PathVariable String schoolId, @PathVariable String id) { canteenService.deleteMenuItem(schoolId, id); return ResponseEntity.ok(ApiResponse.ok("Deleted", null)); }

    @GetMapping("/orders") public ResponseEntity<ApiResponse<List<CanteenOrder>>> getOrders(@PathVariable String schoolId) { return ResponseEntity.ok(ApiResponse.ok(canteenService.getOrders(schoolId))); }
    @PostMapping("/orders") public ResponseEntity<ApiResponse<CanteenOrder>> createOrder(@PathVariable String schoolId, @RequestBody CanteenOrder order) { return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(canteenService.createOrder(schoolId, order))); }

    @GetMapping("/daily-menu") public ResponseEntity<ApiResponse<List<DailyMenu>>> getDailyMenu(@PathVariable String schoolId, @RequestParam(required = false) String date) {
        LocalDate parsed = date != null && !date.isBlank() ? LocalDate.parse(date) : null;
        return ResponseEntity.ok(ApiResponse.ok(canteenService.getDailyMenu(schoolId, parsed)));
    }
    @PostMapping("/daily-menu") public ResponseEntity<ApiResponse<DailyMenu>> saveDailyMenu(@PathVariable String schoolId, @RequestBody DailyMenu menu) { return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(canteenService.saveDailyMenu(schoolId, menu))); }
    @DeleteMapping("/daily-menu/{id}") public ResponseEntity<ApiResponse<Void>> deleteDailyMenu(@PathVariable String schoolId, @PathVariable String id) { canteenService.deleteDailyMenu(schoolId, id); return ResponseEntity.ok(ApiResponse.ok("Deleted", null)); }
}
