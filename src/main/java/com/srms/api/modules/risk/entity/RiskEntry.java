package com.srms.api.modules.risk.entity;

import com.srms.api.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name = "risk_entries") @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RiskEntry extends BaseEntity {
    @Column(nullable = false) private String schoolId;
    private String title;
    private String category; // Financial, Operational, Compliance, Reputational, Safety
    private String likelihood; // Low, Medium, High
    private String impact; // Low, Medium, High
    private String status; // Open, Mitigating, Closed
    private String owner;
    @Column(columnDefinition = "TEXT") private String mitigation;
    private String reviewFrequency;
    private String mitigationDeadline;
    private String nextReviewDate;
    private String actionOwner;
    private String residualLikelihood;
    private String residualImpact;
    private String notes;
}
