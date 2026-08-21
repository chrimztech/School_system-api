package com.srms.api.modules.accounting.entity;

import com.srms.api.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity @Table(name = "chart_accounts", indexes = @Index(name = "idx_chart_accounts_school_id", columnList = "school_id")) @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ChartAccount extends BaseEntity {
    @Column(nullable = false) private String schoolId;
    private String code;
    private String name;
    private String type; // Asset, Liability, Equity, Revenue, Expense
    @Column(precision = 14, scale = 2) private BigDecimal balance;
}
