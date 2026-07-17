package com.srms.api.modules.ptc.entity;
import com.srms.api.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
@Entity @Table(name = "ptc_transactions", indexes = @Index(name = "idx_ptc_transactions_school_id", columnList = "school_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PtcTransaction extends BaseEntity {
    @Column(nullable = false) private String schoolId;
    @Column(nullable = false) private String date;
    private String type; // INCOME, EXPENSE
    private String category;
    private String description;
    @Column(precision = 12, scale = 2) private BigDecimal amount;
    private String recordedBy;
    @Builder.Default private String status = "RECORDED";
}
