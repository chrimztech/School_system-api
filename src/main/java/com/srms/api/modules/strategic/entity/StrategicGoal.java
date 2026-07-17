package com.srms.api.modules.strategic.entity;

import com.srms.api.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name = "strategic_goals", indexes = @Index(name = "idx_strategic_goals_school_id", columnList = "school_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class StrategicGoal extends BaseEntity {
    @Column(nullable = false) private String schoolId;
    private String pillar;
    @Column(nullable = false, length = 300) private String goal;
    private String owner;
    private String deadline;
    private int progress;
    private String status;
    @Column(columnDefinition = "TEXT") private String kpis;
}
