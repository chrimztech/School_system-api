package com.srms.api.modules.fee.controller;
import com.srms.api.common.ApiResponse;
import com.srms.api.common.PageRequestUtil;
import com.srms.api.common.PageResponse;
import com.srms.api.exception.ForbiddenException;
import com.srms.api.modules.fee.entity.FeeBillingRule;
import com.srms.api.modules.fee.entity.FeeDiscountRule;
import com.srms.api.modules.fee.entity.FeeLevy;
import com.srms.api.modules.fee.entity.FeePayment;
import com.srms.api.modules.fee.entity.FeeStructure;
import com.srms.api.modules.fee.service.FeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Set;
@RestController @RequestMapping("/api/schools/{schoolId}/fees") @RequiredArgsConstructor
public class FeeController {
    private final FeeService feeService;

    /** Roles with "full" (not "read"/none) access to fees/fee-structure — everyone else,
     * including PARENT, only ever pays through the gateway-backed self-service flow. */
    private static final Set<String> CAN_MANAGE_FEES_ROLES = Set.of(
            "SUPER_ADMIN", "SCHOOL_ADMIN", "FINANCE", "PRINCIPAL", "DEPUTY_HEAD");

    private static String roleOf(Authentication auth) {
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst().map(a -> a.replaceFirst("^ROLE_", "")).orElse("");
    }

    @GetMapping("/payments")
    public ResponseEntity<ApiResponse<?>> getPayments(
            @PathVariable String schoolId,
            @RequestParam(required = false) Integer page, @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sortBy, @RequestParam(required = false) String sortDir) {
        Pageable pageable = PageRequestUtil.build(page, size, sortBy, sortDir);
        if (pageable == null) return ResponseEntity.ok(ApiResponse.ok(feeService.getAllPayments(schoolId)));
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.of(feeService.getAllPaymentsPaged(schoolId, pageable))));
    }
    @GetMapping("/payments/student/{studentId}") public ResponseEntity<ApiResponse<List<FeePayment>>> getStudentPayments(@PathVariable String schoolId, @PathVariable String studentId) { return ResponseEntity.ok(ApiResponse.ok(feeService.getStudentPayments(schoolId, studentId))); }
    @PostMapping("/payments")
    public ResponseEntity<ApiResponse<FeePayment>> recordPayment(@PathVariable String schoolId, @RequestBody FeePayment payment, Authentication auth) {
        if (!CAN_MANAGE_FEES_ROLES.contains(roleOf(auth))) {
            throw new ForbiddenException("Your role does not have permission to record payments manually — use the online payment option instead");
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(feeService.recordPayment(schoolId, payment)));
    }
    @GetMapping("/collected") public ResponseEntity<ApiResponse<Double>> getCollected(@PathVariable String schoolId) { return ResponseEntity.ok(ApiResponse.ok(feeService.getTotalCollected(schoolId))); }
    @GetMapping("/structures") public ResponseEntity<ApiResponse<List<FeeStructure>>> getStructures(@PathVariable String schoolId) { return ResponseEntity.ok(ApiResponse.ok(feeService.getFeeStructures(schoolId))); }
    @PostMapping("/structures") public ResponseEntity<ApiResponse<FeeStructure>> createStructure(@PathVariable String schoolId, @RequestBody FeeStructure fs, Authentication auth) { requireCanManage(auth); return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(feeService.createFeeStructure(schoolId, fs))); }
    @PatchMapping("/structures/{id}") public ResponseEntity<ApiResponse<FeeStructure>> updateStructure(@PathVariable String schoolId, @PathVariable String id, @RequestBody FeeStructure patch, Authentication auth) { requireCanManage(auth); return ResponseEntity.ok(ApiResponse.ok(feeService.updateFeeStructure(schoolId, id, patch))); }

    @GetMapping("/levies") public ResponseEntity<ApiResponse<List<FeeLevy>>> getLevies(@PathVariable String schoolId) { return ResponseEntity.ok(ApiResponse.ok(feeService.getLevies(schoolId))); }
    @PostMapping("/levies") public ResponseEntity<ApiResponse<FeeLevy>> createLevy(@PathVariable String schoolId, @RequestBody FeeLevy levy, Authentication auth) { requireCanManage(auth); return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(feeService.createLevy(schoolId, levy))); }
    @DeleteMapping("/levies/{id}") public ResponseEntity<ApiResponse<Void>> deleteLevy(@PathVariable String schoolId, @PathVariable String id, Authentication auth) { requireCanManage(auth); feeService.deleteLevy(schoolId, id); return ResponseEntity.ok(ApiResponse.ok(null)); }

    @GetMapping("/discounts") public ResponseEntity<ApiResponse<List<FeeDiscountRule>>> getDiscountRules(@PathVariable String schoolId) { return ResponseEntity.ok(ApiResponse.ok(feeService.getDiscountRules(schoolId))); }
    @PostMapping("/discounts") public ResponseEntity<ApiResponse<FeeDiscountRule>> createDiscountRule(@PathVariable String schoolId, @RequestBody FeeDiscountRule rule, Authentication auth) { requireCanManage(auth); return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(feeService.createDiscountRule(schoolId, rule))); }
    @PatchMapping("/discounts/{id}") public ResponseEntity<ApiResponse<FeeDiscountRule>> updateDiscountRule(@PathVariable String schoolId, @PathVariable String id, @RequestBody FeeDiscountRule patch, Authentication auth) { requireCanManage(auth); return ResponseEntity.ok(ApiResponse.ok(feeService.updateDiscountRule(schoolId, id, patch))); }

    @GetMapping("/billing-rules") public ResponseEntity<ApiResponse<List<FeeBillingRule>>> getBillingRules(@PathVariable String schoolId) { return ResponseEntity.ok(ApiResponse.ok(feeService.getBillingRules(schoolId))); }
    @PostMapping("/billing-rules") public ResponseEntity<ApiResponse<FeeBillingRule>> createBillingRule(@PathVariable String schoolId, @RequestBody FeeBillingRule rule, Authentication auth) { requireCanManage(auth); return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(feeService.createBillingRule(schoolId, rule))); }
    @PatchMapping("/billing-rules/{id}") public ResponseEntity<ApiResponse<FeeBillingRule>> updateBillingRule(@PathVariable String schoolId, @PathVariable String id, @RequestBody FeeBillingRule patch, Authentication auth) { requireCanManage(auth); return ResponseEntity.ok(ApiResponse.ok(feeService.updateBillingRule(schoolId, id, patch))); }

    private void requireCanManage(Authentication auth) {
        if (!CAN_MANAGE_FEES_ROLES.contains(roleOf(auth))) {
            throw new ForbiddenException("Your role does not have permission to manage fee structures");
        }
    }
}
