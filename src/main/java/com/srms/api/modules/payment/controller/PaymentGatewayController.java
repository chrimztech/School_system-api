package com.srms.api.modules.payment.controller;

import com.srms.api.common.ApiResponse;
import com.srms.api.common.GuardianNames;
import com.srms.api.common.PhoneUtils;
import com.srms.api.exception.ForbiddenException;
import com.srms.api.modules.auth.entity.AppUser;
import com.srms.api.modules.auth.repository.UserRepository;
import com.srms.api.modules.fee.entity.FeePayment;
import com.srms.api.modules.payment.dto.CardPaymentRequest;
import com.srms.api.modules.payment.dto.MomoPaymentRequest;
import com.srms.api.modules.payment.service.PaymentGatewayService;
import com.srms.api.modules.student.entity.Student;
import com.srms.api.modules.student.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/schools/{schoolId}/fees/payments")
@RequiredArgsConstructor
public class PaymentGatewayController {

    private final PaymentGatewayService paymentGatewayService;
    private final UserRepository userRepository;
    private final StudentRepository studentRepository;

    private static String roleOf(Authentication auth) {
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst().map(a -> a.replaceFirst("^ROLE_", "")).orElse("");
    }

    @GetMapping("/gateway-status")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> gatewayStatus() {
        return ResponseEntity.ok(ApiResponse.ok(Map.of("available", paymentGatewayService.isGatewayAvailable())));
    }

    @PostMapping("/card/initiate")
    public ResponseEntity<ApiResponse<Map<String, Object>>> initiateCard(@PathVariable String schoolId, @RequestBody CardPaymentRequest request, Authentication auth) {
        if ("PARENT".equals(roleOf(auth))) assertParentOwnsStudent(schoolId, request.getStudentId(), auth);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(paymentGatewayService.initiateCardPayment(schoolId, request)));
    }

    @PostMapping("/momo/initiate")
    public ResponseEntity<ApiResponse<Map<String, Object>>> initiateMomo(@PathVariable String schoolId, @RequestBody MomoPaymentRequest request, Authentication auth) {
        if ("PARENT".equals(roleOf(auth))) assertParentOwnsStudent(schoolId, request.getStudentId(), auth);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(paymentGatewayService.initiateMomoPayment(schoolId, request)));
    }

    @GetMapping("/{paymentId}/status")
    public ResponseEntity<ApiResponse<FeePayment>> status(@PathVariable String schoolId, @PathVariable String paymentId, Authentication auth) {
        FeePayment payment = paymentGatewayService.checkPaymentStatus(schoolId, paymentId);
        if ("PARENT".equals(roleOf(auth))) assertParentOwnsStudent(schoolId, payment.getStudentId(), auth);
        return ResponseEntity.ok(ApiResponse.ok(payment));
    }

    /** Same ownership rule as Fee/Attendance/Assessment/StudentController's identically-named
     * check — a parent may only pay, or check the status of a payment, for their own child. */
    private void assertParentOwnsStudent(String schoolId, String studentId, Authentication auth) {
        AppUser user = userRepository.findById(auth.getName())
                .orElseThrow(() -> new ForbiddenException("Authenticated parent was not found"));
        Student student = studentRepository.findByIdAndSchoolId(studentId, schoolId)
                .orElseThrow(() -> new ForbiddenException("Learner is not available to this parent"));
        // A placeholder guardian name (never actually captured) means the matching phone/email
        // below can't be trusted as proof this is the same family — see GuardianNames' javadoc.
        if (GuardianNames.isPlaceholder(student.getGuardian())) {
            throw new ForbiddenException("Parents can only make or check payments for their own children");
        }
        boolean emailMatch = user.getEmail() != null && student.getGuardianEmail() != null
                && user.getEmail().equalsIgnoreCase(student.getGuardianEmail());
        boolean phoneMatch = user.getPhone() != null && student.getGuardianPhone() != null
                && PhoneUtils.normalize(user.getPhone()).equalsIgnoreCase(PhoneUtils.normalize(student.getGuardianPhone()));
        if (!emailMatch && !phoneMatch) {
            throw new ForbiddenException("Parents can only make or check payments for their own children");
        }
    }
}
