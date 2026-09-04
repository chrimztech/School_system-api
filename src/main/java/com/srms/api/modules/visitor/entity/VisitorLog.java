package com.srms.api.modules.visitor.entity;

import com.srms.api.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity @Table(name = "visitor_logs", indexes = @Index(name = "idx_visitor_logs_school_id", columnList = "school_id")) @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
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
    private String organisation;
    // Gate-register compliance fields — who they are and whether their ID was actually checked,
    // the two most legally-relevant fields on a visitor log.
    private String nationalId;
    private Boolean idChecked;
    private String itemsBrought;
    private String appointmentRef;
    // Distinct from checkInTime (server-set to the moment they actually arrive): these capture
    // an expected/scheduled visit ahead of time.
    private LocalDate visitDate;
    private LocalTime visitTime;
    private LocalTime expectedCheckOutTime;
}
