package com.srms.api.modules.curriculum.entity;

import com.srms.api.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** One teaching week inside a scheme of work. */
@Entity
@Table(name = "scheme_weeks", indexes = @Index(name = "idx_scheme_weeks_scheme", columnList = "scheme_id"))
@Getter
@Setter
@NoArgsConstructor
public class SchemeWeek extends BaseEntity {
    @Column(name = "school_id", nullable = false) private String schoolId;
    @Column(name = "scheme_id", nullable = false) private String schemeId;
    @Column(name = "week_number", nullable = false) private Integer weekNumber;
    @Column(columnDefinition = "TEXT") private String topic;
    @Column(name = "sub_topics", columnDefinition = "TEXT") private String subTopics;
    @Column(columnDefinition = "TEXT") private String objectives;
    @Column(columnDefinition = "TEXT") private String activities;
    @Column(columnDefinition = "TEXT") private String resources;
    @Column(columnDefinition = "TEXT") private String assessment;
    @Column(columnDefinition = "TEXT") private String remarks;
}
