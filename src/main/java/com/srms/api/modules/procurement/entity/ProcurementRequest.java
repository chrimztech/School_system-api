package com.srms.api.modules.procurement.entity;

import com.srms.api.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity @Table(name = "procurement_requests", indexes = @Index(name = "idx_procurement_requests_school_id", columnList = "school_id")) @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProcurementRequest extends BaseEntity {
    @Column(nullable = false) private String schoolId;
    private String requester;
    private String department;
    private String item;
    private Integer quantity;
    private BigDecimal amount;
    private String priority; // Critical, Standard, Low
    private String status; // Draft, Pending approval, Approved, Ordered
    private String vendor;
    private String needByDate;
    private String budgetCode;
    private String deliveryPoint;
    @Column(columnDefinition = "TEXT") private String justification;
}
