package com.srms.api.modules.payment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.srms.api.exception.BusinessException;
import com.srms.api.exception.ResourceNotFoundException;
import com.srms.api.modules.communication.service.NotificationService;
import com.srms.api.modules.fee.entity.FeePayment;
import com.srms.api.modules.fee.repository.FeePaymentRepository;
import com.srms.api.modules.fee.service.FeeService;
import com.srms.api.modules.payment.dto.CardPaymentRequest;
import com.srms.api.modules.payment.dto.MerchantBalanceView;
import com.srms.api.modules.payment.dto.MomoPaymentRequest;
import com.srms.api.modules.payment.dto.PaymentStatusView;
import com.srms.api.modules.payment.entity.PaymentCallbackLog;
import com.srms.api.modules.payment.repository.PaymentCallbackLogRepository;
import com.srms.api.modules.student.entity.Student;
import com.srms.api.modules.student.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PaymentGatewayService {

    private final ZynlePayClient zynlePayClient;
    private final FeePaymentRepository feePaymentRepository;
    private final PaymentCallbackLogRepository callbackLogRepository;
    private final StudentRepository studentRepository;
    private final FeeService feeService;
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    public boolean isGatewayAvailable() {
        return zynlePayClient.isConfigured();
    }

    private void assertGatewayAvailable() {
        if (!zynlePayClient.isConfigured()) {
            throw new BusinessException("Online payment isn't connected for this school yet — please pay at the school office or ask them for other payment options.");
        }
    }

    public Map<String, Object> initiateCardPayment(String schoolId, CardPaymentRequest req) {
        assertGatewayAvailable();
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
        assertGatewayAvailable();
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

    /**
     * Fast synchronous half of callback handling: persist the raw payload for audit purposes
     * (so a disputed payment can always be traced to exactly what ZynlePay sent) and return
     * immediately. ZynlePay expects a quick HTTP 200 — the actual gateway re-verification happens
     * in {@link #verifyCallbackAsync}, kicked off by the controller right after this returns.
     */
    public String recordCallback(Map<String, Object> payload) {
        Object refObj = payload.get("reference_no");
        String referenceNo = refObj != null ? String.valueOf(refObj) : null;
        try {
            callbackLogRepository.save(PaymentCallbackLog.builder()
                    .referenceNumber(referenceNo)
                    .responseCode(String.valueOf(payload.get("response_code")))
                    .rawPayload(objectMapper.writeValueAsString(payload))
                    .build());
        } catch (Exception e) {
            log.warn("Failed to persist raw ZynlePay callback payload: {}", e.getMessage());
        }
        if (referenceNo == null) {
            log.warn("ZynlePay callback missing reference_no: {}", payload);
        }
        return referenceNo;
    }

    /** Don't trust the callback body's status directly — re-verify against the gateway. */
    @Async
    public void verifyCallbackAsync(String referenceNo) {
        FeePayment payment = feePaymentRepository.findByReferenceNumber(referenceNo).orElse(null);
        if (payment == null) {
            log.warn("ZynlePay callback for unknown reference_no: {}", referenceNo);
            return;
        }
        refreshIfPending(payment);
    }

    public FeePayment checkPaymentStatus(String schoolId, String paymentId) {
        FeePayment payment = feePaymentRepository.findById(paymentId)
                .filter(p -> p.getSchoolId().equals(schoolId))
                .orElseThrow(() -> new ResourceNotFoundException("Payment", paymentId));
        refreshIfPending(payment);
        return payment;
    }

    public PaymentStatusView publicStatus(String referenceNo) {
        FeePayment payment = feePaymentRepository.findByReferenceNumber(referenceNo)
                .orElseThrow(() -> new ResourceNotFoundException("Payment reference not found"));
        refreshIfPending(payment);
        return new PaymentStatusView(payment.getStatus().name(), payment.getAmount(), payment.getStudentName(), payment.getReferenceNumber());
    }

    private void refreshIfPending(FeePayment payment) {
        if (payment.getStatus() != FeePayment.PaymentStatus.pending) {
            log.info("Skipping status refresh for {} — already {}", payment.getReferenceNumber(), payment.getStatus());
            return;
        }
        Map<String, Object> statusResponse = zynlePayClient.checkStatus(payment.getReferenceNumber());
        applyGatewayStatus(payment, statusResponse);
    }

    /**
     * Called from three places that can race each other on the same payment: the ZynlePay callback,
     * the frontend's own status poll, and the scheduled reconciliation sweep below. The transition
     * itself is an atomic "UPDATE ... WHERE status = 'pending'" so only one caller ever wins and
     * applies the balance change — the others see 0 rows affected and back off.
     */
    private void applyGatewayStatus(FeePayment payment, Map<String, Object> statusResponse) {
        String code = String.valueOf(statusResponse.get("response_code"));
        if ("100".equals(code)) {
            transitionIfPending(payment, FeePayment.PaymentStatus.completed, code, true);
        } else if ("995".equals(code)) {
            transitionIfPending(payment, FeePayment.PaymentStatus.failed, code, false);
        } else {
            // 990 (pending) or any other transient/unknown code — no status change, just record the latest code.
            feePaymentRepository.updateGatewayResponseCode(payment.getId(), code);
            payment.setGatewayResponseCode(code);
        }
    }

    private void transitionIfPending(FeePayment payment, FeePayment.PaymentStatus newStatus, String code, boolean applyBalanceOnSuccess) {
        int updated = feePaymentRepository.markStatusIfPending(payment.getId(), newStatus, code);
        if (updated == 0) {
            log.info("Payment {} already settled by a concurrent update — skipping duplicate transition", payment.getId());
            feePaymentRepository.findById(payment.getId()).ifPresent(fresh -> {
                payment.setStatus(fresh.getStatus());
                payment.setGatewayResponseCode(fresh.getGatewayResponseCode());
            });
            return;
        }
        payment.setStatus(newStatus);
        payment.setGatewayResponseCode(code);
        if (applyBalanceOnSuccess) {
            feeService.applyToStudentBalance(payment);
            sendReceiptBestEffort(payment);
        }
    }

    private void sendReceiptBestEffort(FeePayment payment) {
        try {
            studentRepository.findByIdAndSchoolId(payment.getStudentId(), payment.getSchoolId()).ifPresent(student ->
                    notificationService.sendPaymentReceipt(payment.getSchoolId(), student.getGuardianEmail(), student.getGuardianPhone(),
                            payment.getStudentName(), payment.getAmount(), payment.getReferenceNumber()));
        } catch (Exception e) {
            log.warn("Failed to send payment receipt for {}: {}", payment.getId(), e.getMessage());
        }
    }

    /**
     * Safety net for the gap between "ZynlePay says it's initiated" and "ZynlePay tells us what
     * happened": if the callback never arrives (dropped webhook, parent closed the app before the
     * frontend's own poll could pick it up), this periodically re-checks any payment that's still
     * pending a minute or more after it started.
     */
    @Scheduled(fixedDelay = 120_000)
    public void reconcilePendingGatewayPayments() {
        List<FeePayment> stale = feePaymentRepository.findByStatusAndGatewayProviderIsNotNullAndCreatedAtBefore(
                FeePayment.PaymentStatus.pending, LocalDateTime.now().minusMinutes(1));
        if (stale.isEmpty()) return;
        log.info("Reconciling {} pending gateway payment(s)", stale.size());
        for (FeePayment payment : stale) {
            try {
                refreshIfPending(payment);
            } catch (Exception e) {
                log.warn("Reconciliation check failed for payment {}: {}", payment.getId(), e.getMessage());
            }
        }
    }

    public MerchantBalanceView getMerchantBalance() {
        assertGatewayAvailable();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("method", "checkBalance");
        Map<String, Object> response = zynlePayClient.postToGateway(null, data);
        return new MerchantBalanceView(
                String.valueOf(response.getOrDefault("merchant_information", "")),
                String.valueOf(response.getOrDefault("disbursement_balance", "0")),
                String.valueOf(response.getOrDefault("collection_balance", "0")));
    }
}
