package com.srms.api.modules.academic.entity;

import com.srms.api.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/** When a term actually starts and ends, so "which term are we in" can be read off a
 *  calendar date instead of relying purely on a manually-set current-term number. */
@Entity
@Table(name = "academic_terms", indexes = @Index(name = "idx_academic_terms_school_id", columnList = "school_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AcademicTerm extends BaseEntity {
    @Column(nullable = false) private String schoolId;
    @Column(nullable = false) private int academicYear;
    @Column(nullable = false) private int term;
    @Column(nullable = false) private LocalDate startDate;
    @Column(nullable = false) private LocalDate endDate;
    private String name;
}
