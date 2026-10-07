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

import java.time.LocalDate;

/** A single lesson, usually generated from one week of a scheme and then edited by the teacher. */
@Entity
@Table(name = "lesson_plans", indexes = @Index(name = "idx_lesson_plans_lookup", columnList = "school_id, class_id, lesson_date"))
@Getter
@Setter
@NoArgsConstructor
public class LessonPlan extends BaseEntity {
    public enum Status { DRAFT, FINAL }
    public enum Source { TEMPLATE, AI, MANUAL }

    @Column(name = "school_id", nullable = false) private String schoolId;
    @Column(name = "scheme_id") private String schemeId;
    @Column(name = "scheme_week_id") private String schemeWeekId;
    @Column(name = "class_id", nullable = false) private String classId;
    @Column(name = "subject_name", nullable = false) private String subjectName;
    @Column(name = "lesson_date") private LocalDate lessonDate;
    @Column(name = "duration_minutes") private Integer durationMinutes;
    @Column(nullable = false, columnDefinition = "TEXT") private String topic;
    @Column(name = "previous_knowledge", columnDefinition = "TEXT") private String previousKnowledge;
    @Column(columnDefinition = "TEXT") private String objectives;
    @Column(columnDefinition = "TEXT") private String materials;
    @Column(columnDefinition = "TEXT") private String introduction;
    @Column(columnDefinition = "TEXT") private String development;
    @Column(columnDefinition = "TEXT") private String conclusion;
    @Column(columnDefinition = "TEXT") private String evaluation;
    @Column(columnDefinition = "TEXT") private String homework;
    /** Filled in by the teacher after the lesson is taught — reflection notes and, when
     * supervised, the observing HOD/head's sign-off comment. Never set by generation. */
    @Column(name = "teacher_remarks", columnDefinition = "TEXT") private String teacherRemarks;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private Status status = Status.DRAFT;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private Source source = Source.MANUAL;
    @Column(name = "created_by") private String createdBy;
}
