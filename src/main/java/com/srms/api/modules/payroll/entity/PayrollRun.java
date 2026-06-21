package com.srms.api.modules.payroll.entity;

import com.srms.api.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity @Table(name = "payroll_runs") @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PayrollRun extends BaseEntity {
    @Column(nullable = false) private String schoolId;
    private int month;
    private int year;
    private LocalDate runDate;
    @Column(precision = 14, scale = 2) private BigDecimal totalGross;
    @Column(precision = 14, scale = 2) private BigDecimal totalNet;
    private String status; // DRAFT, PROCESSED, PAID
    private int staffCount;
}
