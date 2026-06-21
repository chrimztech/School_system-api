package com.srms.api.modules.academic.entity;

import com.srms.api.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "class_enrolments",
    uniqueConstraints = @UniqueConstraint(columnNames = {"class_id", "student_id", "academic_year"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ClassEnrolment extends BaseEntity {
    @Column(name = "school_id", nullable = false) private String schoolId;
    @Column(name = "class_id", nullable = false) private String classId;
    @Column(name = "student_id", nullable = false) private String studentId;
    private String studentName;
    private String grade;
    private String academicYear;
    @Builder.Default private String status = "ACTIVE"; // ACTIVE, TRANSFERRED, WITHDRAWN
}
