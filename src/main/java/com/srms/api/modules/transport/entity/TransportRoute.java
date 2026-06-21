package com.srms.api.modules.transport.entity;

import com.srms.api.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "transport_routes")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TransportRoute extends BaseEntity {

    @Column(nullable = false)
    private String schoolId;

    @Column(nullable = false)
    private String routeName;

    private String description;

    @Column(columnDefinition = "TEXT")
    private String stops;

    private String departureTime;
    private String arrivalTime;
    private String vehicleId;

    @Column(precision = 10, scale = 2)
    private BigDecimal feePerTerm;
}
