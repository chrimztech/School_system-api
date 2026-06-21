package com.srms.api.modules.hostel.entity;

import com.srms.api.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity @Table(name = "hostel_leaves") @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class HostelLeave extends BaseEntity {
    @Column(nullable = false) private String schoolId;
    private String studentName;
    private String studentId;
    private String leaveType;
    private LocalDate fromDate;
    private LocalDate toDate;
    private String destination;
    private String contactAtDestination;
    private String parentApprovalRef;
    private String transportArrangement;
    private String guardianPhone;
    @Builder.Default private String status = "PENDING";
}
