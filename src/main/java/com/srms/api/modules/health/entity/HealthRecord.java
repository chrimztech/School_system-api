package com.srms.api.modules.health.entity;

import com.srms.api.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity @Table(name = "health_records") @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class HealthRecord extends BaseEntity {
    @Column(nullable = false) private String schoolId;
    private String studentId;
    private String studentName;
    private String grade;
    private String bloodGroup;
    @Column(columnDefinition = "TEXT") private String allergies;
    @Column(columnDefinition = "TEXT") private String chronicConditions;
    private String emergencyContact;
    private String emergencyPhone;
    private LocalDate lastCheckupDate;
    private String vaccinationStatus;
    @Column(columnDefinition = "TEXT") private String notes;
}
