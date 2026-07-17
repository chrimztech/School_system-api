package com.srms.api.modules.welfare.entity;

import com.srms.api.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name = "counseling_sessions", indexes = @Index(name = "idx_counseling_sessions_school_id", columnList = "school_id")) @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CounselingSession extends BaseEntity {
    @Column(nullable = false) private String schoolId;
    private String student;
    private String counselor;
    private String sessionDate;
    private String sessionType; // Individual, Group, Parent
    @Column(columnDefinition = "TEXT") private String notes;
}
