package com.srms.api.modules.report.entity;

import com.srms.api.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name = "report_comments",
    uniqueConstraints = @UniqueConstraint(columnNames = {"school_id", "student_id", "term", "academic_year"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ReportComment extends BaseEntity {
    @Column(name = "school_id", nullable = false) private String schoolId;
    @Column(name = "student_id", nullable = false) private String studentId;
    @Column(nullable = false) private String term;
    @Column(name = "academic_year", nullable = false) private String academicYear;
    @Column(columnDefinition = "TEXT") private String teacherComment;
    @Column(columnDefinition = "TEXT") private String headComment;
}
