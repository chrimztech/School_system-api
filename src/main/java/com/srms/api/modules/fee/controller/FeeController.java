package com.srms.api.modules.fee.controller;
import com.srms.api.common.ApiResponse;
import com.srms.api.modules.fee.entity.FeeBillingRule;
import com.srms.api.modules.fee.entity.FeeDiscountRule;
import com.srms.api.modules.fee.entity.FeeLevy;
import com.srms.api.modules.fee.entity.FeePayment;
import com.srms.api.modules.fee.entity.FeeStructure;
import com.srms.api.modules.fee.service.FeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController @RequestMapping("/api/schools/{schoolId}/fees") @RequiredArgsConstructor
public class FeeController {
    private final FeeService feeService;
    @GetMapping("/payments") public ResponseEntity<ApiResponse<List<FeePayment>>> getPayments(@PathVariable String schoolId) { return ResponseEntity.ok(ApiResponse.ok(feeService.getAllPayments(schoolId))); }
    @GetMapping("/payments/student/{studentId}") public ResponseEntity<ApiResponse<List<FeePayment>>> getStudentPayments(@PathVariable String schoolId, @PathVariable String studentId) { return ResponseEntity.ok(ApiResponse.ok(feeService.getStudentPayments(schoolId, studentId))); }
    @PostMapping("/payments") public ResponseEntity<ApiResponse<FeePayment>> recordPayment(@PathVariable String schoolId, @RequestBody FeePayment payment) { return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(feeService.recordPayment(schoolId, payment))); }
    @GetMapping("/collected") public ResponseEntity<ApiResponse<Double>> getCollected(@PathVariable String schoolId) { return ResponseEntity.ok(ApiResponse.ok(feeService.getTotalCollected(schoolId))); }
    @GetMapping("/structures") public ResponseEntity<ApiResponse<List<FeeStructure>>> getStructures(@PathVariable String schoolId) { return ResponseEntity.ok(ApiResponse.ok(feeService.getFeeStructures(schoolId))); }
    @PostMapping("/structures") public ResponseEntity<ApiResponse<FeeStructure>> createStructure(@PathVariable String schoolId, @RequestBody FeeStructure fs) { return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(feeService.createFeeStructure(schoolId, fs))); }
    @PatchMapping("/structures/{id}") public ResponseEntity<ApiResponse<FeeStructure>> updateStructure(@PathVariable String schoolId, @PathVariable String id, @RequestBody FeeStructure patch) { return ResponseEntity.ok(ApiResponse.ok(feeService.updateFeeStructure(schoolId, id, patch))); }

    @GetMapping("/levies") public ResponseEntity<ApiResponse<List<FeeLevy>>> getLevies(@PathVariable String schoolId) { return ResponseEntity.ok(ApiResponse.ok(feeService.getLevies(schoolId))); }
    @PostMapping("/levies") public ResponseEntity<ApiResponse<FeeLevy>> createLevy(@PathVariable String schoolId, @RequestBody FeeLevy levy) { return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(feeService.createLevy(schoolId, levy))); }
    @DeleteMapping("/levies/{id}") public ResponseEntity<ApiResponse<Void>> deleteLevy(@PathVariable String schoolId, @PathVariable String id) { feeService.deleteLevy(schoolId, id); return ResponseEntity.ok(ApiResponse.ok(null)); }

    @GetMapping("/discounts") public ResponseEntity<ApiResponse<List<FeeDiscountRule>>> getDiscountRules(@PathVariable String schoolId) { return ResponseEntity.ok(ApiResponse.ok(feeService.getDiscountRules(schoolId))); }
    @PostMapping("/discounts") public ResponseEntity<ApiResponse<FeeDiscountRule>> createDiscountRule(@PathVariable String schoolId, @RequestBody FeeDiscountRule rule) { return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(feeService.createDiscountRule(schoolId, rule))); }
    @PatchMapping("/discounts/{id}") public ResponseEntity<ApiResponse<FeeDiscountRule>> updateDiscountRule(@PathVariable String schoolId, @PathVariable String id, @RequestBody FeeDiscountRule patch) { return ResponseEntity.ok(ApiResponse.ok(feeService.updateDiscountRule(schoolId, id, patch))); }

    @GetMapping("/billing-rules") public ResponseEntity<ApiResponse<List<FeeBillingRule>>> getBillingRules(@PathVariable String schoolId) { return ResponseEntity.ok(ApiResponse.ok(feeService.getBillingRules(schoolId))); }
    @PostMapping("/billing-rules") public ResponseEntity<ApiResponse<FeeBillingRule>> createBillingRule(@PathVariable String schoolId, @RequestBody FeeBillingRule rule) { return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(feeService.createBillingRule(schoolId, rule))); }
    @PatchMapping("/billing-rules/{id}") public ResponseEntity<ApiResponse<FeeBillingRule>> updateBillingRule(@PathVariable String schoolId, @PathVariable String id, @RequestBody FeeBillingRule patch) { return ResponseEntity.ok(ApiResponse.ok(feeService.updateBillingRule(schoolId, id, patch))); }
}
