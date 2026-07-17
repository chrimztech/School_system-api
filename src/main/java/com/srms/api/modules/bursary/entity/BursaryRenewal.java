package com.srms.api.modules.bursary.entity;

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
@Table(name = "bursary_renewals", indexes = @Index(name = "idx_bursary_renewals_school_id", columnList = "school_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BursaryRenewal extends BaseEntity {
    @Column(nullable = false)
    private String schoolId;

    private String student;
    private String sponsor;
    private String reviewDate;
    private String attendance;
    private String academics;
    private String status;
}
