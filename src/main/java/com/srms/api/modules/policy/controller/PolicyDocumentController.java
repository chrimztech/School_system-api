package com.srms.api.modules.policy.controller;

import com.srms.api.common.ApiResponse;
import com.srms.api.modules.policy.entity.PolicyDocument;
import com.srms.api.modules.policy.service.PolicyDocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @RequestMapping("/api/schools/{schoolId}/policy-documents") @RequiredArgsConstructor
public class PolicyDocumentController {
    private final PolicyDocumentService service;

    @GetMapping public ResponseEntity<ApiResponse<List<PolicyDocument>>> list(@PathVariable String schoolId) {
        return ResponseEntity.ok(ApiResponse.ok(service.list(schoolId)));
    }

    @PostMapping public ResponseEntity<ApiResponse<PolicyDocument>> create(@PathVariable String schoolId, @RequestBody PolicyDocument doc) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(service.create(schoolId, doc)));
    }

    @PutMapping("/{id}") public ResponseEntity<ApiResponse<PolicyDocument>> update(@PathVariable String schoolId, @PathVariable String id, @RequestBody PolicyDocument doc) {
        return ResponseEntity.ok(ApiResponse.ok(service.update(schoolId, id, doc)));
    }

    @DeleteMapping("/{id}") public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String schoolId, @PathVariable String id) {
        service.delete(schoolId, id);
        return ResponseEntity.ok(ApiResponse.ok("Policy document deleted", null));
    }
}
