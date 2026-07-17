package com.srms.api.modules.academic.entity;
import com.srms.api.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
@Entity @Table(name = "subjects", indexes = @Index(name = "idx_subjects_school_id", columnList = "school_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Subject extends BaseEntity {
    @Column(nullable = false) private String schoolId;
    @Column(nullable = false) private String name;
    private String code;
    private String department;
    private Integer gradeFrom;
    private Integer gradeTo;
    private boolean compulsory;
    /** "primary" | "junior" | "senior" */
    private String phase;
    private Integer periodsPerWeek;
    @Column(columnDefinition = "TEXT") private String description;
    @Builder.Default private boolean active = true;
}
