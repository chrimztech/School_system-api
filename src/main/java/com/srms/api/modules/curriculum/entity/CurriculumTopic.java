package com.srms.api.modules.curriculum.entity;

import com.srms.api.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** One topic in a subject's syllabus for a grade and term, in teaching order. */
@Entity
@Table(name = "curriculum_topics", indexes = @Index(name = "idx_curriculum_topics_lookup", columnList = "school_id, subject_name, grade, term"))
@Getter
@Setter
@NoArgsConstructor
public class CurriculumTopic extends BaseEntity {
    @Column(name = "school_id", nullable = false) private String schoolId;
    @Column(name = "subject_name", nullable = false) private String subjectName;
    @Column(nullable = false) private Integer grade;
    @Column(nullable = false) private String term;
    @Column(nullable = false) private Integer sequence;
    @Column(nullable = false, columnDefinition = "TEXT") private String topic;
    @Column(name = "sub_topics", columnDefinition = "TEXT") private String subTopics;
    @Column(columnDefinition = "TEXT") private String objectives;
    @Column(columnDefinition = "TEXT") private String activities;
    @Column(columnDefinition = "TEXT") private String resources;
    @Column(columnDefinition = "TEXT") private String assessment;
}
