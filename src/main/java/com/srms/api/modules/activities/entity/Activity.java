package com.srms.api.modules.activities.entity;

import com.srms.api.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name = "activities", indexes = @Index(name = "idx_activities_school_id", columnList = "school_id")) @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Activity extends BaseEntity {
    @Column(nullable = false) private String schoolId;
    @Column(nullable = false) private String name;
    private String type; // SPORT, CLUB, CULTURAL, ACADEMIC, COMMUNITY
    private String category;
    @Column(columnDefinition = "TEXT") private String description;
    private String coordinator;
    private String venue;
    private String meetingSchedule;
    private String meetingDay;
    private String meetingTime;
    private String status; // ACTIVE, INACTIVE
    private String targetGroup;
    private int maxParticipants;
    private Integer meetingDuration;
    @Column(precision = 12, scale = 2) private java.math.BigDecimal budgetAllocated;
    @Column(precision = 10, scale = 2) private java.math.BigDecimal membershipFee;
    @Builder.Default
    @Column(columnDefinition = "boolean default false") private Boolean insuranceRequired = false;
    private String startDate;
    private String clubConstitutionRef;
}
