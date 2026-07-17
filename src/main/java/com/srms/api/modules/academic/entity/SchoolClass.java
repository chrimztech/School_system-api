package com.srms.api.modules.academic.entity;
import com.srms.api.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
@Entity @Table(name = "school_classes", indexes = @Index(name = "idx_school_classes_school_id", columnList = "school_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SchoolClass extends BaseEntity {
    @Column(nullable = false) private String schoolId;
    @Column(nullable = false) private String name;
    private String section;
    private int grade;
    private String classTeacherId;
    private String classTeacherName;
    private int capacity;
    private int currentEnrolment;
    private String room;
    private boolean active = true;
    private String academicYear;
    private String phase;
    @Column(columnDefinition = "TEXT") private String subjects;
    private Integer timetableSlotsPerWeek;
    private String assistantTeacher;
    private String languageOfInstruction;
    private String assessmentStream;
    private String notes;
}
