package com.srms.api.modules.development.entity;

import com.srms.api.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Index;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "staff_appraisals", indexes = @Index(name = "idx_staff_appraisals_school_id", columnList = "school_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StaffAppraisal extends BaseEntity {
    @Column(nullable = false)
    private String schoolId;

    private String staff;
    private String role;
    private String reviewer;
    private String cycle;
    private Integer score;
    private String status;
    private String appraisalMethod;
    private String startDate;
    private String competencyFocus;
    private Integer targetScore;
}
