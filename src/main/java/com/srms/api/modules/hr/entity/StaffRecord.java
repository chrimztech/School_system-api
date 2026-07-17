package com.srms.api.modules.hr.entity;

import com.srms.api.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "staff_records", indexes = @Index(name = "idx_staff_records_school_id", columnList = "school_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class StaffRecord extends BaseEntity {

    @Column(nullable = false)
    private String schoolId;

    private String userId;

    @Column(nullable = false, unique = true)
    private String staffNumber;

    private String department;
    private String position;

    @Column(columnDefinition = "TEXT")
    private String qualifications;

    @Enumerated(EnumType.STRING)
    private ContractType contractType;

    private LocalDate hireDate;

    @Column(precision = 12, scale = 2)
    private BigDecimal salary;

    @Enumerated(EnumType.STRING)
    private StaffStatus status;

    private String tpin;
    private String paymentMethod;
    @Builder.Default
    @Column(columnDefinition = "boolean default false") private Boolean napsaEnrolled = false;
    private String bankName;
    private String accountNumber;
    private String emergencyContactName;
    private String emergencyContactPhone;

    public enum ContractType { PERMANENT, CONTRACT, PART_TIME }

    public enum StaffStatus { ACTIVE, ON_LEAVE, INACTIVE, TERMINATED }
}
