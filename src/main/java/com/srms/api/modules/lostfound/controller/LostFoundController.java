package com.srms.api.modules.lostfound.controller;

import com.srms.api.common.ApiResponse;
import com.srms.api.modules.lostfound.entity.LostFoundItem;
import com.srms.api.modules.lostfound.service.LostFoundService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController @RequestMapping("/api/schools/{schoolId}/lost-found") @RequiredArgsConstructor
public class LostFoundController {
    private final LostFoundService service;
    @GetMapping public ResponseEntity<ApiResponse<List<LostFoundItem>>> getAll(@PathVariable String schoolId) { return ResponseEntity.ok(ApiResponse.ok(service.getAll(schoolId))); }
    @PostMapping public ResponseEntity<ApiResponse<LostFoundItem>> create(@PathVariable String schoolId, @RequestBody LostFoundItem item) { return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(service.create(schoolId, item))); }
    @PutMapping("/{id}") public ResponseEntity<ApiResponse<LostFoundItem>> update(@PathVariable String schoolId, @PathVariable String id, @RequestBody LostFoundItem item) { return ResponseEntity.ok(ApiResponse.ok(service.update(schoolId, id, item))); }
    @PutMapping("/{id}/claim") public ResponseEntity<ApiResponse<LostFoundItem>> claim(@PathVariable String schoolId, @PathVariable String id, @RequestBody Map<String, String> body) { return ResponseEntity.ok(ApiResponse.ok(service.claim(schoolId, id, body.get("ownerName"), body.get("ownerContact")))); }
}
