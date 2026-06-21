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
@Table(name = "staff_observations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StaffObservation extends BaseEntity {
    @Column(nullable = false)
    private String schoolId;

    private String observer;
    private String observee;
    private String subject;
    private String grade;
    private String date;
    private Integer rating;
    private String time;
    private String lessonObjective;
    private String strengthsObserved;
    private String areasForImprovement;

    @Column(columnDefinition = "TEXT")
    private String notes;
}
