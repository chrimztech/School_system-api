package com.srms.api.modules.activities.entity;

import com.srms.api.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity @Table(name = "activity_enrolments") @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ActivityEnrolment extends BaseEntity {
    @Column(nullable = false) private String schoolId;
    private String activityId;
    private String activityName;
    private String studentId;
    private String studentName;
    private String grade;
    private LocalDate enrolmentDate;
    private String status; // ACTIVE, WITHDRAWN
}
