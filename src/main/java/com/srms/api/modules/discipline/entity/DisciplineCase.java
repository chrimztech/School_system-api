package com.srms.api.modules.discipline.entity;

import com.srms.api.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "discipline_cases")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DisciplineCase extends BaseEntity {
    @Column(nullable = false) private String schoolId;
    @Column(nullable = false) private String studentId;
    private String studentName;
    private String grade;
    private String offense;
    private String offenseCategory;
    private String severity;
    private String action; // VERBAL_WARNING, WRITTEN_WARNING, SUSPENSION, EXPULSION
    private LocalDate incidentDate;
    private String incidentTime;
    private String location;
    private String witnessNames;
    private LocalDate followUpDate;
    private boolean parentNotified;
    private int repeatCount;
    private String reportedBy;
    private String status; // OPEN, RESOLVED, ESCALATED
    @Column(columnDefinition = "TEXT") private String notes;
}
