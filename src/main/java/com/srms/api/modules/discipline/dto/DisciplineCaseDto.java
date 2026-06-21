package com.srms.api.modules.discipline.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class DisciplineCaseDto {
    private String id;
    private String schoolId;
    private String studentId;
    private String studentName;
    private String grade;
    private String offense;
    private String offenseCategory;
    private String severity;
    private String action;
    private LocalDate incidentDate;
    private String incidentTime;
    private String location;
    private String witnessNames;
    private LocalDate followUpDate;
    private boolean parentNotified;
    private int repeatCount;
    private String reportedBy;
    private String status;
    private String notes;
}
