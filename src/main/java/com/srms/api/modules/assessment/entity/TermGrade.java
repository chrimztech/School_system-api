package com.srms.api.modules.assessment.entity;
import com.srms.api.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name = "term_grades",
    uniqueConstraints = @UniqueConstraint(columnNames = {"school_id", "student_id", "subject_name", "term", "academic_year"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TermGrade extends BaseEntity {
    @Column(name = "school_id", nullable = false) private String schoolId;
    @Column(name = "student_id", nullable = false) private String studentId;
    private String studentName;
    @Column(name = "class_id") private String classId;
    @Column(name = "subject_name", nullable = false) private String subjectName;
    @Column(nullable = false) private String term;
    @Column(name = "academic_year", nullable = false) private String academicYear;
    private Double caPercent;
    private Double midtermPercent;
    private Double examPercent;
    private double weightedTotal;
    private String letterGrade;
    private String gradeDescription;
    private Integer gradePoints;
    private boolean complete;
    @Builder.Default private boolean published = false;
    private String teacherId;
}
