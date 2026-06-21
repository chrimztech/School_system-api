package com.srms.api.modules.accounting.entity;

import com.srms.api.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity @Table(name = "journal_entries")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class JournalEntry extends BaseEntity {
    @Column(nullable = false) private String schoolId;
    @Column(nullable = false) private LocalDate entryDate;
    private String reference;
    @Column(length = 500) private String description;
    @Column(nullable = false) private double debitAmount;
    @Column(nullable = false) private double creditAmount;
    private String debitAccount;
    private String creditAccount;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JournalStatus status;

    public enum JournalStatus { DRAFT, POSTED }
}
