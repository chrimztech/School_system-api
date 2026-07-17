package com.srms.api.modules.fee.entity;

import com.srms.api.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Index;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "fee_discount_rules", indexes = @Index(name = "idx_fee_discount_rules_school_id", columnList = "school_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeeDiscountRule extends BaseEntity {
    @Column(nullable = false)
    private String schoolId;

    @Column(nullable = false)
    private String name;

    private String type;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal value;

    @Column(columnDefinition = "TEXT")
    private String condition;

    @Builder.Default
    @Column(columnDefinition = "boolean default true")
    private Boolean active = true;

    private Integer maxBeneficiaries;
    private String validFrom;
    private String validTo;

    @Builder.Default
    @Column(columnDefinition = "boolean default false")
    private Boolean requiresBoardApproval = false;
}
