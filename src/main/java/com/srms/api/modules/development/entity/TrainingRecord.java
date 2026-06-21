package com.srms.api.modules.development.entity;

import com.srms.api.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name = "training_records") @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TrainingRecord extends BaseEntity {
    @Column(nullable = false) private String schoolId;
    private String staffName;
    private String program;
    private String provider;
    private String startDate;
    private String endDate;
    private String status; // Planned, In progress, Completed, Cancelled
    private int hours;
    private String category; // CPD, Leadership, Curriculum, Safeguarding, ICT
    private String venue;
    private String mode;
    @Builder.Default
    @Column(columnDefinition = "boolean default false") private Boolean certificationIssued = false;
    private String certificationRef;
    @Column(precision = 12, scale = 2) private java.math.BigDecimal trainingBudget;
}
