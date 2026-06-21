package com.srms.api.modules.visitor.entity;

import com.srms.api.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity @Table(name = "visitor_logs") @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class VisitorLog extends BaseEntity {
    @Column(nullable = false) private String schoolId;
    private String visitorName;
    private String visitorPhone;
    private String purposeOfVisit;
    private String hostName;
    private String hostType; // STUDENT, TEACHER, ADMIN
    private LocalDateTime checkInTime;
    private LocalDateTime checkOutTime;
    private String badgeNumber;
    private String vehicleReg;
    private String status; // CHECKED_IN, CHECKED_OUT
}
