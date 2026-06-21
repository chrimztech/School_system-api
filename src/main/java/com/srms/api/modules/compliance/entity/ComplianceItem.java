package com.srms.api.modules.compliance.entity;

import com.srms.api.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name = "compliance_items") @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ComplianceItem extends BaseEntity {
    @Column(nullable = false) private String schoolId;
    private String title;
    private String category; // Regulatory, Policy, Health & Safety, HR, Finance
    private String status; // Compliant, Pending, Non-compliant, Exempt
    private String dueDate;
    private String owner;
    @Column(columnDefinition = "TEXT") private String notes;
}
