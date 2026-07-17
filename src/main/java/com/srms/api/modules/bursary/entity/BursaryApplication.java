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

import java.math.BigDecimal;

@Entity
@Table(name = "bursary_applications", indexes = @Index(name = "idx_bursary_applications_school_id", columnList = "school_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BursaryApplication extends BaseEntity {
    @Column(nullable = false)
    private String schoolId;

    private String student;
    private String household;

    @Column(precision = 12, scale = 2)
    private BigDecimal requested;

    @Column(columnDefinition = "TEXT")
    private String reason;

    private String status;
}
