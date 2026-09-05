package com.srms.api.modules.facility.entity;

import com.srms.api.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name = "facility_assets", indexes = @Index(name = "idx_facility_assets_school_id", columnList = "school_id")) @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Asset extends BaseEntity {
    @Column(nullable = false) private String schoolId;
    private String name;
    private String category; // Furniture, Electronics, Vehicle, Appliance, Equipment...
    private String location;
    private String condition; // Good, Fair, Poor, Under repair, Decommissioned
    private String serialNumber;
    private String purchaseDate;
    private String warrantyExpiry;
    private Double value;
    @Column(columnDefinition = "TEXT") private String notes;
}
