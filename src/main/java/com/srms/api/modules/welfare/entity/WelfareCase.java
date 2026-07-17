package com.srms.api.modules.welfare.entity;

import com.srms.api.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name = "welfare_cases", indexes = @Index(name = "idx_welfare_cases_school_id", columnList = "school_id")) @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class WelfareCase extends BaseEntity {
    @Column(nullable = false) private String schoolId;
    private String student;
    private String grade;
    private String type; // Academic, Emotional, Social, Family, Medical
    private String assignedTo;
    private String status; // Open, Monitoring, Resolved
    private String lastContact;
}
