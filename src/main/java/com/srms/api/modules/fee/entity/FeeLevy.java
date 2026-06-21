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
@Table(name = "fee_levies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeeLevy extends BaseEntity {
    @Column(nullable = false)
    private String schoolId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    private String grade;

    @Builder.Default
    @Column(columnDefinition = "boolean default true")
    private Boolean mandatory = true;

    private String description;
    private String applicableTo;
    private String effectiveFrom;
}
