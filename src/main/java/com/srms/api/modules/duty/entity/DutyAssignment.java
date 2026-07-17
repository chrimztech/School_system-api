package com.srms.api.modules.duty.entity;

import com.srms.api.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name = "duty_assignments", indexes = @Index(name = "idx_duty_assignments_school_id", columnList = "school_id")) @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DutyAssignment extends BaseEntity {
    @Column(nullable = false) private String schoolId;
    private String staffName;
    private String role;
    private String dayOfWeek; // Monday...Sunday
    private String location;
    private String startTime;
    private String endTime;
    private String week; // e.g. "Week 1", "Week 2"
    private String term;
    private String backupStaff;
    private String effectiveDate;
    private String rotationCycle;
    private String notes;
    private String approvalStatus;
}
