package com.srms.api.modules.fee.entity;
import com.srms.api.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
@Entity @Table(name = "fee_payments")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FeePayment extends BaseEntity {
    @Column(nullable = false) private String schoolId;
    @Column(nullable = false) private String studentId;
    private String studentName;
    private String grade;
    @Column(nullable = false) private double amount;
    private String method;
    private LocalDate paymentDate;
    private String referenceNumber;
    @Enumerated(EnumType.STRING) private PaymentStatus status;
    private String description;
    private String collectedBy;
    private String receiptNumber;
    private String feeCategory;
    private String termPeriod;
    private boolean latePenaltyApplied;
    private double penaltyAmount;
    public enum PaymentStatus { completed, pending, failed, reversed }
}
