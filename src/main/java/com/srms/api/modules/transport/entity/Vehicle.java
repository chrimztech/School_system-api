package com.srms.api.modules.transport.entity;

import com.srms.api.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "vehicles", indexes = @Index(name = "idx_vehicles_school_id", columnList = "school_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Vehicle extends BaseEntity {

    public enum Status { ACTIVE, MAINTENANCE, INACTIVE }

    @Column(nullable = false)
    private String schoolId;

    @Column(unique = true)
    private String plateNumber;

    private String make;
    private String model;
    private int capacity;
    private String driverName;
    private String driverPhone;
    private String routeName;

    @Enumerated(EnumType.STRING)
    private Status status = Status.ACTIVE;
}
