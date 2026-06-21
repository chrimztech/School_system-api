package com.srms.api.modules.development.entity;

import com.srms.api.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "staff_development_plans")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StaffDevelopmentPlan extends BaseEntity {
    @Column(nullable = false)
    private String schoolId;

    private String staff;
    private Integer goals;
    private String nextReview;
    private String status;
    private String reviewType;
    private String developmentArea;
    private String supportRequired;

    @Column(length = 300)
    private String goal1;

    @Column(length = 300)
    private String goal2;

    @Column(length = 300)
    private String goal3;
}
