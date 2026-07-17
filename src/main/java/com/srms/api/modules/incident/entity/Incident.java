package com.srms.api.modules.incident.entity;

import com.srms.api.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name = "incidents", indexes = @Index(name = "idx_incidents_school_id", columnList = "school_id")) @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Incident extends BaseEntity {
    @Column(nullable = false) private String schoolId;
    private String title;
    private String type; // Safety, Discipline, Health, Property, Cyber, Other
    private String location;
    private String reportedBy;
    private String incidentDate;
    private String status; // Reported, Investigating, Resolved, Closed
    @Column(columnDefinition = "TEXT") private String description;
    private String severity; // Low, Medium, High, Critical
    private String incidentTime;
    @Builder.Default
    @Column(columnDefinition = "boolean default false") private Boolean injuryOccurred = false;
    private String injuredParty;
    private String investigationAssignedTo;
    @Column(columnDefinition = "TEXT") private String rootCauseAnalysis;
    @Column(columnDefinition = "TEXT") private String preventiveAction;
    @Builder.Default
    @Column(columnDefinition = "boolean default false") private Boolean parentNotified = false;
    private String evidenceRef;
}
