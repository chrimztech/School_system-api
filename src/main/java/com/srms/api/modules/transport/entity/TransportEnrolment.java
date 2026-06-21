package com.srms.api.modules.transport.entity;

import com.srms.api.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "transport_enrolments")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TransportEnrolment extends BaseEntity {

    public enum Status { ACTIVE, INACTIVE }

    @Column(nullable = false)
    private String schoolId;

    private String studentId;
    private String studentName;
    private String grade;
    private String routeId;
    private String routeName;
    private String pickupStop;
    private String term;
    private String academicYear;

    @Enumerated(EnumType.STRING)
    private Status status = Status.ACTIVE;
}
