package com.srms.api.modules.fee.entity;

import com.srms.api.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "fee_billing_rules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeeBillingRule extends BaseEntity {
    @Column(nullable = false)
    private String schoolId;

    @Column(nullable = false)
    private String term;

    private String dueDate;

    @Column(precision = 12, scale = 2)
    private BigDecimal lateFee;

    private Integer reminderDays;
    private Integer gracePeriodDays;
    private String lateFeeMethod;

    @Column(precision = 12, scale = 2)
    private BigDecimal maxPenalty;
}
