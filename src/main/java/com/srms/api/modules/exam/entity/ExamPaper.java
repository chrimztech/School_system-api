package com.srms.api.modules.exam.entity;

import com.srms.api.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name = "exam_papers", indexes = @Index(name = "idx_exam_papers_school_id", columnList = "school_id")) @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ExamPaper extends BaseEntity {
    @Column(nullable = false) private String schoolId;
    private String code;
    private String subject;
    private String grade;
    private String examDate;
    private String startTime;
    private String duration;
    private String room;
    private int candidates;
    private String invigilator;
    private String status; // SCHEDULED, IN_PROGRESS, COMPLETED
}
