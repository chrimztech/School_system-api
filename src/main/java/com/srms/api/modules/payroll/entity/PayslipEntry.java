package com.srms.api.modules.payroll.entity;

import com.srms.api.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity @Table(name = "payslip_entries", indexes = @Index(name = "idx_payslip_entries_school_id", columnList = "school_id")) @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PayslipEntry extends BaseEntity {
    @Column(nullable = false) private String schoolId;
    private String payrollRunId;
    private String staffId;
    private String staffName;
    private String position;
    @Column(precision = 12, scale = 2) private BigDecimal grossSalary;
    @Column(precision = 10, scale = 2) private BigDecimal napsa;
    @Column(precision = 10, scale = 2) private BigDecimal paye;
    @Column(precision = 10, scale = 2) private BigDecimal nhima;
    @Column(precision = 10, scale = 2) private BigDecimal otherDeductions;
    @Column(precision = 12, scale = 2) private BigDecimal netSalary;
}
