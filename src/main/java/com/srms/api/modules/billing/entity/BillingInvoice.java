package com.srms.api.modules.billing.entity;

import com.srms.api.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name = "billing_invoices", indexes = @Index(name = "idx_billing_invoices_school_id", columnList = "school_id")) @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BillingInvoice extends BaseEntity {
    @Column(nullable = false) private String schoolId;
    @Column(nullable = false) private String invoiceNumber;
    @Column(nullable = false) private String description;
    @Column(nullable = false) private Double amount;
    @Builder.Default private String status = "open"; // open, paid, overdue
    private String dueDate;
    private String paidDate;
    private String plan;
    private String billingCycle;
}
