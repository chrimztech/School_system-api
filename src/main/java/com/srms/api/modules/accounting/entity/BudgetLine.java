package com.srms.api.modules.accounting.entity;

import com.srms.api.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity @Table(name = "budget_lines", indexes = @Index(name = "idx_budget_lines_school_id", columnList = "school_id")) @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BudgetLine extends BaseEntity {
    @Column(nullable = false) private String schoolId;
    private String category;
    @Column(precision = 14, scale = 2) private BigDecimal allocated;
    @Column(precision = 14, scale = 2) private BigDecimal spent;
    private String term;
    private Integer academicYear;
}
