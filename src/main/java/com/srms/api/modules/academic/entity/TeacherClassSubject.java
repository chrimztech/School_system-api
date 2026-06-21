package com.srms.api.modules.academic.entity;

import com.srms.api.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "teacher_class_subjects")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TeacherClassSubject extends BaseEntity {
    @Column(name = "school_id", nullable = false) private String schoolId;
    @Column(name = "class_id", nullable = false) private String classId;
    private String className;
    @Column(name = "teacher_id", nullable = false) private String teacherId;
    private String teacherName;
    private String subjectId;
    private String subjectName;
    private String academicYear;
}
