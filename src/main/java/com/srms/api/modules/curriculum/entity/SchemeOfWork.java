package com.srms.api.modules.curriculum.entity;

import com.srms.api.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** A class's week-by-week plan for one subject in one term, moving through review before use. */
@Entity
@Table(name = "schemes_of_work", indexes = @Index(name = "idx_schemes_lookup", columnList = "school_id, class_id, subject_name, term"))
@Getter
@Setter
@NoArgsConstructor
public class SchemeOfWork extends BaseEntity {
    public enum Status { DRAFT, SUBMITTED, APPROVED, REJECTED }

    @Column(name = "school_id", nullable = false) private String schoolId;
    @Column(name = "class_id", nullable = false) private String classId;
    @Column(name = "class_name") private String className;
    @Column(name = "subject_name", nullable = false) private String subjectName;
    @Column(nullable = false) private String term;
    @Column(name = "academic_year", nullable = false) private String academicYear;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private Status status = Status.DRAFT;
    @Column(name = "created_by") private String createdBy;
    @Column(name = "submitted_at") private LocalDateTime submittedAt;
    @Column(name = "reviewed_by") private String reviewedBy;
    @Column(name = "reviewed_at") private LocalDateTime reviewedAt;
    @Column(name = "review_note", columnDefinition = "TEXT") private String reviewNote;
}
