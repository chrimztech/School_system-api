package com.srms.api.modules.policy.entity;

import com.srms.api.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name = "policy_documents", indexes = @Index(name = "idx_policy_documents_school_id", columnList = "school_id")) @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PolicyDocument extends BaseEntity {
    @Column(nullable = false) private String schoolId;
    private String title;
    private String category; // Governance, Safety, Procurement, Discipline, Finance, ICT
    private String status; // Review, Approved, Draft
    private String uploadedBy;
    // Base64 data URL, same storage pattern as School.logoUrl — no separate file storage yet.
    @Column(columnDefinition = "TEXT") private String fileUrl;
    private String fileName;
}
