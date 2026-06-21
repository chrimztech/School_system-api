package com.srms.api.modules.accounting.entity;

import com.srms.api.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity @Table(name = "expenses")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Expense extends BaseEntity {
    @Column(nullable = false) private String schoolId;
    @Column(nullable = false) private LocalDate expenseDate;
    @Column(nullable = false) private String vendor;
    private String category;
    @Column(nullable = false) private double amount;
    private String paymentMethod;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExpenseStatus status;

    public enum ExpenseStatus { PAID, PENDING }
}
