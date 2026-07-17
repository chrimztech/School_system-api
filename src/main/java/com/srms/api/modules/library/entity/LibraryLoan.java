package com.srms.api.modules.library.entity;

import com.srms.api.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "library_loans", indexes = @Index(name = "idx_library_loans_school_id", columnList = "school_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LibraryLoan extends BaseEntity {

    public enum BorrowerType { STUDENT, STAFF }
    public enum Status { ACTIVE, RETURNED, OVERDUE }

    @Column(nullable = false)
    private String schoolId;

    private String bookId;
    private String bookTitle;

    @Enumerated(EnumType.STRING)
    private BorrowerType borrowerType;

    private String borrowerId;
    private String borrowerName;

    private LocalDate loanDate;
    private LocalDate dueDate;
    private LocalDate returnDate;

    @Enumerated(EnumType.STRING)
    private Status status = Status.ACTIVE;

    @Column(precision = 10, scale = 2)
    private BigDecimal fineAmount;
}
