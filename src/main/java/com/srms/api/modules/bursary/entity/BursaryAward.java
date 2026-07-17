package com.srms.api.modules.bursary.entity;

import com.srms.api.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity @Table(name = "bursary_awards", indexes = @Index(name = "idx_bursary_awards_school_id", columnList = "school_id")) @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BursaryAward extends BaseEntity {
    @Column(nullable = false) private String schoolId;
    private String student;
    private String grade;
    private String sponsor;
    private String coverage; // Full, Partial, Fees only
    private BigDecimal amount;
    private String status; // Active, Pending renewal, Closed
    @Column(columnDefinition = "TEXT")
    private String applicationReason;
    private String household; // income level
    private String startDate;
    private String endDate;
    private String sponsorshipAgreementRef;
    private String disbursementSchedule;
    private String performanceConditions;
}
