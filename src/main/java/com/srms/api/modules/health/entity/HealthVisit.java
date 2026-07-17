package com.srms.api.modules.health.entity;

import com.srms.api.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity @Table(name = "health_visits", indexes = @Index(name = "idx_health_visits_school_id", columnList = "school_id")) @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class HealthVisit extends BaseEntity {
    @Column(nullable = false) private String schoolId;
    private String studentId;
    private String studentName;
    private String grade;
    private LocalDate visitDate;
    private String complaint;
    private String diagnosis;
    @Column(columnDefinition = "TEXT") private String treatment;
    private boolean referredToHospital;
    private String attendedBy;
    private LocalDate followUpDate;
}
