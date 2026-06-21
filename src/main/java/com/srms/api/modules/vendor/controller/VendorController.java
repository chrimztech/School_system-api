package com.srms.api.modules.vendor.controller;

import com.srms.api.common.ApiResponse;
import com.srms.api.modules.vendor.entity.Vendor;
import com.srms.api.modules.vendor.service.VendorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @RequestMapping("/api/schools/{schoolId}/vendors") @RequiredArgsConstructor
public class VendorController {
    private final VendorService vendorService;
    @GetMapping public ResponseEntity<ApiResponse<List<Vendor>>> list(@PathVariable String schoolId) { return ResponseEntity.ok(ApiResponse.ok(vendorService.list(schoolId))); }
    @PostMapping public ResponseEntity<ApiResponse<Vendor>> create(@PathVariable String schoolId, @RequestBody Vendor v) { return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(vendorService.create(schoolId, v))); }
    @PutMapping("/{id}") public ResponseEntity<ApiResponse<Vendor>> update(@PathVariable String schoolId, @PathVariable String id, @RequestBody Vendor v) { return ResponseEntity.ok(ApiResponse.ok(vendorService.update(schoolId, id, v))); }
}
