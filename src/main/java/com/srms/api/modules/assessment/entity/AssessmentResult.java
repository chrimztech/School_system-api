package com.srms.api.modules.assessment.entity;
import com.srms.api.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
@Entity @Table(name = "assessment_results", indexes = @Index(name = "idx_assessment_results_school_id", columnList = "school_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AssessmentResult extends BaseEntity {
    @Column(nullable = false) private String schoolId;
    @Column(nullable = false) private String assessmentId;
    @Column(nullable = false) private String studentId;
    private String studentName;
    private double score;
    private String grade;
    private String remarks;
    private boolean absent;
}
