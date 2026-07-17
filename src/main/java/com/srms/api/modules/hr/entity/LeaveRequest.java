package com.srms.api.modules.hr.entity;

import com.srms.api.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "leave_requests", indexes = @Index(name = "idx_leave_requests_school_id", columnList = "school_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LeaveRequest extends BaseEntity {

    @Column(nullable = false)
    private String schoolId;

    private String staffId;
    private String staffName;

    @Enumerated(EnumType.STRING)
    private LeaveType leaveType;

    private LocalDate startDate;
    private LocalDate endDate;
    private int days;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Enumerated(EnumType.STRING)
    private LeaveStatus status;

    private String approvedBy;
    private LocalDate appliedDate;

    public enum LeaveType { ANNUAL, SICK, MATERNITY, PATERNITY, STUDY, EMERGENCY }

    public enum LeaveStatus { PENDING, APPROVED, REJECTED }
}
