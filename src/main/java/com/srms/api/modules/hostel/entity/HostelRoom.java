package com.srms.api.modules.hostel.entity;

import com.srms.api.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name = "hostel_rooms") @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class HostelRoom extends BaseEntity {
    @Column(nullable = false) private String schoolId;
    private String roomNumber;
    private String hostelName;
    private String roomType; // DORMITORY, SEMI_PRIVATE, PRIVATE
    private int capacity;
    private int occupiedBeds;
    private String gender; // MALE, FEMALE, MIXED
    private String floor;
    private String status; // ACTIVE, MAINTENANCE
}
