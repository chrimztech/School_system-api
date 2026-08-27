package com.srms.api.modules.fee.entity;
import com.srms.api.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
@Entity @Table(name = "fee_structures", indexes = @Index(name = "idx_fee_structures_school_id", columnList = "school_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FeeStructure extends BaseEntity {
    @Column(nullable = false) private String schoolId;
    @Column(nullable = false) private String name;
    private String description;
    private int gradeFrom;
    private int gradeTo;
    private double termFee;
    private double annualFee;
    private String term;
    private int academicYear;
    private boolean active = true;
    private String dueDate;
    private Double latePenaltyAmount;
    private Integer penaltyGraceDays;
    private String notes;

    /** "DAY", "BOARDING", or null/blank for "applies to every student regardless of
     *  boarding status" — a Boarding-category item left null would otherwise get billed
     *  to day scholars too. */
    private String boardingStatus;
}
