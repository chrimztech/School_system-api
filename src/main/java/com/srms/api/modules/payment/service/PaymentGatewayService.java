package com.srms.api.modules.payment.service;

import com.srms.api.exception.BusinessException;
import com.srms.api.exception.ResourceNotFoundException;
import com.srms.api.modules.fee.entity.FeePayment;
import com.srms.api.modules.fee.repository.FeePaymentRepository;
import com.srms.api.modules.fee.service.FeeService;
import com.srms.api.modules.payment.dto.CardPaymentRequest;
import com.srms.api.modules.payment.dto.MomoPaymentRequest;
import com.srms.api.modules.payment.dto.PaymentStatusView;
import com.srms.api.modules.student.entity.Student;
import com.srms.api.modules.student.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PaymentGatewayService {

    private final ZynlePayClient zynlePayClient;
    private final FeePaymentRepository feePaymentRepository;
    private final StudentRepository studentRepository;
    private final FeeService feeService;

    public Map<String, Object> initiateCardPayment(String schoolId, CardPaymentRequest req) {
        if (req.getAmount() <= 0) {
            throw new BusinessException("Payment amount must be greater than zero");
        }
        Student student = studentRepository.findByIdAndSchoolId(req.getStudentId(), schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("Student", req.getStudentId()));
        String studentName = (student.getFirstName() + " " + student.getLastName()).trim();
        String referenceNo = generateReferenceNo();

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("method", "runTranAuthCapture");
        data.put("reference_no", referenceNo);
        data.put("amount", String.valueOf(req.getAmount()));
        data.put("description", "School fees payment for " + studentName);
        data.put("first_name", req.getFirstName());
        data.put("last_name", req.getLastName());
        data.put("address", req.getAddress());
        data.put("email", req.getEmail());
        data.put("phone", req.getPhone());
        data.put("city", req.getCity());
        data.put("state", req.getState());
        data.put("currency", "ZMW");
        data.put("zip_code", req.getZipCode());
        data.put("country", "ZMB");

        Map<String, Object> response = zynlePayClient.postToGateway("card", data);
        FeePayment saved = savePendingPayment(schoolId, student, studentName, req.getAmount(), "card",
                "Card payment via ZynlePay", referenceNo, response);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("paymentId", saved.getId());
        result.put("referenceNumber", referenceNo);
        result.put("redirectUrl", response.get("redirect_url"));
        return result;
    }

    public Map<String, Object> initiateMomoPayment(String schoolId, MomoPaymentRequest req) {
        if (req.getAmount() <= 0) {
            throw new BusinessException("Payment amount must be greater than zero");
        }
        if (req.getPhoneNumber() == null || req.getPhoneNumber().isBlank()) {
            throw new BusinessException("A mobile money phone number is required");
        }
        Student student = studentRepository.findByIdAndSchoolId(req.getStudentId(), schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("Student", req.getStudentId()));
        String studentName = (student.getFirstName() + " " + student.getLastName()).trim();
        String referenceNo = generateReferenceNo();

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("method", "runBillPayment");
        data.put("sender_id", req.getPhoneNumber());
        data.put("reference_no", referenceNo);
        data.put("amount", String.valueOf(req.getAmount()));

        Map<String, Object> response = zynlePayClient.postToGateway("momo", data);
        FeePayment saved = savePendingPayment(schoolId, student, studentName, req.getAmount(), "momo",
                "Mobile money payment via ZynlePay", referenceNo, response);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("paymentId", saved.getId());
        result.put("referenceNumber", referenceNo);
        result.put("operator", response.get("operator"));
        return result;
    }

    private String generateReferenceNo() {
        return "SRMS" + UUID.randomUUID().toString().replace("-", "").substring(0, 20).toUpperCase();
    }

    private FeePayment savePendingPayment(String schoolId, Student student, String studentName, double amount,
                                           String channel, String description, String referenceNo, Map<String, Object> response) {
        String responseCode = String.valueOf(response.get("response_code"));
        if (!"120".equals(responseCode)) {
            String errorMessage = String.valueOf(response.getOrDefault("response_description", "Payment could not be initiated"));
            throw new BusinessException(errorMessage);
        }

        FeePayment payment = FeePayment.builder()
                .schoolId(schoolId)
                .studentId(student.getId())
                .studentName(studentName)
                .grade(String.valueOf(student.getGrade()))
                .amount(amount)
                .method(channel)
                .paymentDate(LocalDate.now())
                .referenceNumber(referenceNo)
                .status(FeePayment.PaymentStatus.pending)
                .description(description)
                .collectedBy("Parent self-service")
                .gatewayProvider("zynlepay")
                .gatewayChannel(channel)
                .gatewayTransactionId(String.valueOf(response.get("transaction_id")))
                .gatewayResponseCode(responseCode)
                .gatewayRedirectUrl(response.get("redirect_url") != null ? String.valueOf(response.get("redirect_url")) : null)
                .build();
        return feePaymentRepository.save(payment);
    }

    public void handleCallback(Map<String, Object> payload) {
        Object refObj = payload.get("reference_no");
        if (refObj == null) {
            log.warn("ZynlePay callback missing reference_no: {}", payload);
            return;
        }
        String referenceNo = String.valueOf(refObj);
        FeePayment payment = feePaymentRepository.findByReferenceNumber(referenceNo).orElse(null);
        if (payment == null) {
            log.warn("ZynlePay callback for unknown reference_no: {}", referenceNo);
            return;
        }
        if (payment.getStatus() != FeePayment.PaymentStatus.pending) {
            log.info("ZynlePay callback for already-settled reference_no {} (status={}) — ignoring", referenceNo, payment.getStatus());
            return;
        }
        // Don't trust the callback body's status directly — re-verify against the gateway.
        Map<String, Object> statusResponse = zynlePayClient.checkStatus(referenceNo);
        applyGatewayStatus(payment, statusResponse);
    }

    public FeePayment checkPaymentStatus(String schoolId, String paymentId) {
        FeePayment payment = feePaymentRepository.findById(paymentId)
                .filter(p -> p.getSchoolId().equals(schoolId))
                .orElseThrow(() -> new ResourceNotFoundException("Payment", paymentId));
        if (payment.getStatus() == FeePayment.PaymentStatus.pending) {
            Map<String, Object> statusResponse = zynlePayClient.checkStatus(payment.getReferenceNumber());
            applyGatewayStatus(payment, statusResponse);
        }
        return payment;
    }

    public PaymentStatusView publicStatus(String referenceNo) {
        FeePayment payment = feePaymentRepository.findByReferenceNumber(referenceNo)
                .orElseThrow(() -> new ResourceNotFoundException("Payment reference not found"));
        if (payment.getStatus() == FeePayment.PaymentStatus.pending) {
            Map<String, Object> statusResponse = zynlePayClient.checkStatus(referenceNo);
            applyGatewayStatus(payment, statusResponse);
        }
        return new PaymentStatusView(payment.getStatus().name(), payment.getAmount(), payment.getStudentName(), payment.getReferenceNumber());
    }

    private void applyGatewayStatus(FeePayment payment, Map<String, Object> statusResponse) {
        String code = String.valueOf(statusResponse.get("response_code"));
        payment.setGatewayResponseCode(code);
        if ("100".equals(code)) {
            payment.setStatus(FeePayment.PaymentStatus.completed);
            feePaymentRepository.save(payment);
            feeService.applyToStudentBalance(payment);
        } else if ("995".equals(code)) {
            payment.setStatus(FeePayment.PaymentStatus.failed);
            feePaymentRepository.save(payment);
        } else {
            // 990 (pending) or any other transient/unknown code — leave pending, just record the latest code.
            feePaymentRepository.save(payment);
        }
    }
}
