package com.srms.api.modules.facility.entity;

import com.srms.api.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name = "work_orders", indexes = @Index(name = "idx_work_orders_school_id", columnList = "school_id")) @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class WorkOrder extends BaseEntity {
    @Column(nullable = false) private String schoolId;
    private String title;
    private String location;
    private String owner;
    private String priority; // High, Medium, Low
    private String status; // Open, Scheduled, In progress, Closed
    private String dueDate;
    private String workOrderType;
    private String contractorAssigned;
    private Double costEstimate;
    private String budgetCode;
    private String partsRequired;
    private String completionDate;
    @Column(columnDefinition = "TEXT") private String description;
    @Builder.Default
    @Column(columnDefinition = "boolean default false") private Boolean safetyRisk = false;
}
