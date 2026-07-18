package com.srms.api.modules.assessment.entity;

import com.srms.api.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "published_term_grades", uniqueConstraints = @UniqueConstraint(columnNames = {
        "school_id", "student_id", "subject_name", "term", "academic_year", "reporting_period"
}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PublishedTermGrade extends BaseEntity {
    @Column(name = "school_id", nullable = false) private String schoolId;
    @Column(name = "student_id", nullable = false) private String studentId;
    private String studentName;
    @Column(name = "class_id") private String classId;
    @Column(name = "subject_name", nullable = false) private String subjectName;
    @Column(nullable = false) private String term;
    @Column(name = "academic_year", nullable = false) private String academicYear;
    @Enumerated(EnumType.STRING)
    @Column(name = "reporting_period", nullable = false)
    private Assessment.ReportingPeriod reportingPeriod;
    private Double caPercent;
    private Double midtermPercent;
    private Double examPercent;
    private double weightedTotal;
    private String letterGrade;
    private String gradeDescription;
    private Integer gradePoints;
    private String publishedBy;
    private LocalDateTime publishedAt;
}
