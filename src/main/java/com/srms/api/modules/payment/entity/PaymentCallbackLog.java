package com.srms.api.modules.payment.entity;

import com.srms.api.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * Immutable audit record of every raw ZynlePay callback received, saved before any processing —
 * so a disputed or mis-applied payment can always be traced back to exactly what the gateway sent.
 */
@Entity
@Table(name = "payment_callback_logs")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PaymentCallbackLog extends BaseEntity {
    private String referenceNumber;
    private String responseCode;

    @Column(columnDefinition = "TEXT")
    private String rawPayload;
}
