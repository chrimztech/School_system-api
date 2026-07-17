package com.srms.api.modules.hostel.entity;

import com.srms.api.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity @Table(name = "hostel_allocations", indexes = @Index(name = "idx_hostel_allocations_school_id", columnList = "school_id")) @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class HostelAllocation extends BaseEntity {
    @Column(nullable = false) private String schoolId;
    private String studentId;
    private String studentName;
    private String grade;
    private String roomId;
    private String roomNumber;
    private String hostelName;
    private String bedNumber;
    private int term;
    private String academicYear;
    @Column(precision = 10, scale = 2) private BigDecimal feePerTerm;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private String status; // ACTIVE, VACATED
    @Builder.Default
    @Column(columnDefinition = "varchar(3) default 'IN'") private String signInStatus = "IN"; // IN, OUT
    @Builder.Default
    @Column(columnDefinition = "boolean default false") private Boolean keyIssued = false;
    private LocalDate keyIssuedDate;
    private String emergencyContactName;
    private String emergencyContactPhone;
    @Builder.Default
    @Column(columnDefinition = "boolean default false") private Boolean parentalApproval = false;
    private String medicalNeeds;
    private String dietaryRequirements;
}
