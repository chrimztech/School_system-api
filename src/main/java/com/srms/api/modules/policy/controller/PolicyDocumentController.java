package com.srms.api.modules.policy.controller;

import com.srms.api.common.ApiResponse;
import com.srms.api.exception.ForbiddenException;
import com.srms.api.modules.policy.entity.PolicyDocument;
import com.srms.api.modules.policy.service.PolicyDocumentService;
import com.srms.api.security.ModuleAccessService;
import com.srms.api.security.RoleGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Set;

// The frontend's "policy-library" module gives teacher/hod/finance only "read", never full —
// writes are school-leadership only, matching auth.tsx's MODULE_MATRIX exactly.
@RestController @RequestMapping("/api/schools/{schoolId}/policy-documents") @RequiredArgsConstructor
public class PolicyDocumentController {
    private final PolicyDocumentService service;
    private final ModuleAccessService moduleAccessService;

    // Mirrors RoleGuard's own (private) SCHOOL_ACCOUNT_MANAGERS set.
    private static final Set<String> SCHOOL_ACCOUNT_MANAGERS = Set.of(
            "SUPER_ADMIN", "SCHOOL_ADMIN", "PRINCIPAL", "DEPUTY_HEAD");

    @GetMapping public ResponseEntity<ApiResponse<List<PolicyDocument>>> list(@PathVariable String schoolId) {
        return ResponseEntity.ok(ApiResponse.ok(service.list(schoolId)));
    }

    @PostMapping public ResponseEntity<ApiResponse<PolicyDocument>> create(@PathVariable String schoolId, @RequestBody PolicyDocument doc, Authentication auth) {
        requireManage(schoolId, auth);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(service.create(schoolId, doc)));
    }

    @PutMapping("/{id}") public ResponseEntity<ApiResponse<PolicyDocument>> update(@PathVariable String schoolId, @PathVariable String id, @RequestBody PolicyDocument doc, Authentication auth) {
        requireManage(schoolId, auth);
        return ResponseEntity.ok(ApiResponse.ok(service.update(schoolId, id, doc)));
    }

    @DeleteMapping("/{id}") public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String schoolId, @PathVariable String id, Authentication auth) {
        requireManage(schoolId, auth);
        service.delete(schoolId, id);
        return ResponseEntity.ok(ApiResponse.ok("Policy document deleted", null));
    }

    private void requireManage(String schoolId, Authentication auth) {
        if (!moduleAccessService.isAllowed(schoolId, auth, "policy-library", "full", SCHOOL_ACCOUNT_MANAGERS.contains(RoleGuard.roleOf(auth)))) {
            throw new ForbiddenException("Your role cannot manage school accounts or permissions");
        }
    }
}
