package com.srms.api.modules.fee.controller;
import com.srms.api.common.ApiResponse;
import com.srms.api.common.PageRequestUtil;
import com.srms.api.common.PageResponse;
import com.srms.api.common.PhoneUtils;
import com.srms.api.exception.ForbiddenException;
import com.srms.api.modules.auth.entity.AppUser;
import com.srms.api.modules.auth.repository.UserRepository;
import com.srms.api.modules.fee.dto.FeeCollectionSummary;
import com.srms.api.modules.fee.entity.FeeBillingRule;
import com.srms.api.modules.fee.entity.FeeDiscountRule;
import com.srms.api.modules.fee.entity.FeeLevy;
import com.srms.api.modules.fee.entity.FeePayment;
import com.srms.api.modules.fee.entity.FeeStructure;
import com.srms.api.modules.fee.service.FeeService;
import com.srms.api.modules.student.entity.Student;
import com.srms.api.modules.student.repository.StudentRepository;
import com.srms.api.security.ModuleAccessService;
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
    private final ModuleAccessService moduleAccessService;
    private final UserRepository userRepository;
    private final StudentRepository studentRepository;

    /** Roles with "full" (not "read"/none) access to fees/fee-structure — everyone else,
     * including PARENT, only ever pays through the gateway-backed self-service flow. */
    private static final Set<String> CAN_MANAGE_FEES_ROLES = Set.of(
            "SUPER_ADMIN", "SCHOOL_ADMIN", "FINANCE", "PRINCIPAL", "DEPUTY_HEAD");

    private static String roleOf(Authentication auth) {
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst().map(a -> a.replaceFirst("^ROLE_", "")).orElse("");
    }

    // The full school-wide payment ledger — every family's amounts, methods, and reference
    // numbers. Never exposed to PARENT (their portal only ever reads their own children via
    // listByGuardian + getStudentPayments below) or to any role fees marks as "false"/no
    // override; TEACHER included, matching CAN_MANAGE_FEES_ROLES's existing scope elsewhere in
    // this controller.
    @GetMapping("/payments")
    public ResponseEntity<ApiResponse<?>> getPayments(
            @PathVariable String schoolId,
            @RequestParam(required = false) Integer page, @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sortBy, @RequestParam(required = false) String sortDir,
            Authentication auth) {
        if (!moduleAccessService.isAllowed(schoolId, auth, "fees", "read", CAN_MANAGE_FEES_ROLES.contains(roleOf(auth)))) {
            throw new ForbiddenException("Your role cannot access the fee payment ledger");
        }
        Pageable pageable = PageRequestUtil.build(page, size, sortBy, sortDir);
        if (pageable == null) return ResponseEntity.ok(ApiResponse.ok(feeService.getAllPayments(schoolId)));
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.of(feeService.getAllPaymentsPaged(schoolId, pageable))));
    }
    @GetMapping("/payments/student/{studentId}")
    public ResponseEntity<ApiResponse<List<FeePayment>>> getStudentPayments(
            @PathVariable String schoolId, @PathVariable String studentId, Authentication auth) {
        String role = roleOf(auth);
        if ("PARENT".equals(role)) {
            assertParentOwnsStudent(schoolId, studentId, auth);
        } else if (!moduleAccessService.isAllowed(schoolId, auth, "fees", "read", CAN_MANAGE_FEES_ROLES.contains(role))) {
            throw new ForbiddenException("Your role cannot access this pupil's payment history");
        }
        return ResponseEntity.ok(ApiResponse.ok(feeService.getStudentPayments(schoolId, studentId)));
    }

    /** Same ownership rule as AttendanceController/AssessmentController/StudentController's
     * identically-named check. */
    private void assertParentOwnsStudent(String schoolId, String studentId, Authentication auth) {
        AppUser user = userRepository.findById(auth.getName())
                .orElseThrow(() -> new ForbiddenException("Authenticated parent was not found"));
        Student student = studentRepository.findByIdAndSchoolId(studentId, schoolId)
                .orElseThrow(() -> new ForbiddenException("Learner is not available to this parent"));
        // A captured guardian name is not required — the email/phone match below is enough on
        // its own to confirm ownership (product decision: must work even when the pupil's
        // guardian name was never typed in, as long as the contact is attached to the pupil).
        boolean emailMatch = user.getEmail() != null && student.getGuardianEmail() != null
                && user.getEmail().equalsIgnoreCase(student.getGuardianEmail());
        boolean phoneMatch = user.getPhone() != null && student.getGuardianPhone() != null
                && PhoneUtils.normalize(user.getPhone()).equalsIgnoreCase(PhoneUtils.normalize(student.getGuardianPhone()));
        if (!emailMatch && !phoneMatch) {
            throw new ForbiddenException("Parents can only view payments for their own children");
        }
    }
    @PostMapping("/payments")
    public ResponseEntity<ApiResponse<FeePayment>> recordPayment(@PathVariable String schoolId, @RequestBody FeePayment payment, Authentication auth) {
        if (!moduleAccessService.isAllowed(schoolId, auth, "fees", "full", CAN_MANAGE_FEES_ROLES.contains(roleOf(auth)))) {
            throw new ForbiddenException("Your role does not have permission to record payments manually — use the online payment option instead");
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(feeService.recordPayment(schoolId, payment)));
    }
    @PatchMapping("/payments/{id}")
    public ResponseEntity<ApiResponse<FeePayment>> updatePayment(@PathVariable String schoolId, @PathVariable String id, @RequestBody FeePayment patch, Authentication auth) {
        requireCanManage(schoolId, auth, "fees");
        return ResponseEntity.ok(ApiResponse.ok(feeService.updatePayment(schoolId, id, patch)));
    }
    @DeleteMapping("/payments/{id}")
    public ResponseEntity<ApiResponse<FeePayment>> reversePayment(@PathVariable String schoolId, @PathVariable String id, Authentication auth) {
        requireCanManage(schoolId, auth, "fees");
        return ResponseEntity.ok(ApiResponse.ok(feeService.reversePayment(schoolId, id)));
    }
    @GetMapping("/collected") public ResponseEntity<ApiResponse<FeeCollectionSummary>> getCollected(@PathVariable String schoolId) { return ResponseEntity.ok(ApiResponse.ok(feeService.getCollectionSummary(schoolId))); }

    // Re-derives every active student's balance from the fee structures/levies that currently
    // exist, minus payments already on file — see FeeService.recalculateBalances for why this
    // exists (fee structures added after a roster import never retroactively bill anyone
    // without this). Same permission bar as managing fee structures.
    @PostMapping("/recalculate-balances")
    public ResponseEntity<ApiResponse<?>> recalculateBalances(@PathVariable String schoolId, Authentication auth) {
        requireCanManage(schoolId, auth, "fee-structure");
        return ResponseEntity.ok(ApiResponse.ok(feeService.recalculateBalances(schoolId)));
    }
    @GetMapping("/structures") public ResponseEntity<ApiResponse<List<FeeStructure>>> getStructures(@PathVariable String schoolId) { return ResponseEntity.ok(ApiResponse.ok(feeService.getFeeStructures(schoolId))); }
    @PostMapping("/structures") public ResponseEntity<ApiResponse<FeeStructure>> createStructure(@PathVariable String schoolId, @RequestBody FeeStructure fs, Authentication auth) { requireCanManage(schoolId, auth, "fee-structure"); return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(feeService.createFeeStructure(schoolId, fs))); }
    @PatchMapping("/structures/{id}") public ResponseEntity<ApiResponse<FeeStructure>> updateStructure(@PathVariable String schoolId, @PathVariable String id, @RequestBody FeeStructure patch, Authentication auth) { requireCanManage(schoolId, auth, "fee-structure"); return ResponseEntity.ok(ApiResponse.ok(feeService.updateFeeStructure(schoolId, id, patch))); }
    @DeleteMapping("/structures/{id}") public ResponseEntity<ApiResponse<Void>> deleteStructure(@PathVariable String schoolId, @PathVariable String id, Authentication auth) { requireCanManage(schoolId, auth, "fee-structure"); feeService.deleteFeeStructure(schoolId, id); return ResponseEntity.ok(ApiResponse.ok(null)); }

    @GetMapping("/levies") public ResponseEntity<ApiResponse<List<FeeLevy>>> getLevies(@PathVariable String schoolId) { return ResponseEntity.ok(ApiResponse.ok(feeService.getLevies(schoolId))); }
    @PostMapping("/levies") public ResponseEntity<ApiResponse<FeeLevy>> createLevy(@PathVariable String schoolId, @RequestBody FeeLevy levy, Authentication auth) { requireCanManage(schoolId, auth, "fee-structure"); return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(feeService.createLevy(schoolId, levy))); }
    @PatchMapping("/levies/{id}") public ResponseEntity<ApiResponse<FeeLevy>> updateLevy(@PathVariable String schoolId, @PathVariable String id, @RequestBody FeeLevy patch, Authentication auth) { requireCanManage(schoolId, auth, "fee-structure"); return ResponseEntity.ok(ApiResponse.ok(feeService.updateLevy(schoolId, id, patch))); }
    @DeleteMapping("/levies/{id}") public ResponseEntity<ApiResponse<Void>> deleteLevy(@PathVariable String schoolId, @PathVariable String id, Authentication auth) { requireCanManage(schoolId, auth, "fee-structure"); feeService.deleteLevy(schoolId, id); return ResponseEntity.ok(ApiResponse.ok(null)); }

    @GetMapping("/discounts") public ResponseEntity<ApiResponse<List<FeeDiscountRule>>> getDiscountRules(@PathVariable String schoolId) { return ResponseEntity.ok(ApiResponse.ok(feeService.getDiscountRules(schoolId))); }
    @PostMapping("/discounts") public ResponseEntity<ApiResponse<FeeDiscountRule>> createDiscountRule(@PathVariable String schoolId, @RequestBody FeeDiscountRule rule, Authentication auth) { requireCanManage(schoolId, auth, "fee-structure"); return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(feeService.createDiscountRule(schoolId, rule))); }
    @PatchMapping("/discounts/{id}") public ResponseEntity<ApiResponse<FeeDiscountRule>> updateDiscountRule(@PathVariable String schoolId, @PathVariable String id, @RequestBody FeeDiscountRule patch, Authentication auth) { requireCanManage(schoolId, auth, "fee-structure"); return ResponseEntity.ok(ApiResponse.ok(feeService.updateDiscountRule(schoolId, id, patch))); }

    @GetMapping("/billing-rules") public ResponseEntity<ApiResponse<List<FeeBillingRule>>> getBillingRules(@PathVariable String schoolId) { return ResponseEntity.ok(ApiResponse.ok(feeService.getBillingRules(schoolId))); }
    @PostMapping("/billing-rules") public ResponseEntity<ApiResponse<FeeBillingRule>> createBillingRule(@PathVariable String schoolId, @RequestBody FeeBillingRule rule, Authentication auth) { requireCanManage(schoolId, auth, "fee-structure"); return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(feeService.createBillingRule(schoolId, rule))); }
    @PatchMapping("/billing-rules/{id}") public ResponseEntity<ApiResponse<FeeBillingRule>> updateBillingRule(@PathVariable String schoolId, @PathVariable String id, @RequestBody FeeBillingRule patch, Authentication auth) { requireCanManage(schoolId, auth, "fee-structure"); return ResponseEntity.ok(ApiResponse.ok(feeService.updateBillingRule(schoolId, id, patch))); }

    private void requireCanManage(String schoolId, Authentication auth, String module) {
        if (!moduleAccessService.isAllowed(schoolId, auth, module, "full", CAN_MANAGE_FEES_ROLES.contains(roleOf(auth)))) {
            throw new ForbiddenException("Your role does not have permission to manage fee structures");
        }
    }
}
