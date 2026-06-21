package com.srms.api.modules.strategic.entity;

import com.srms.api.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name = "strategic_reviews")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class StrategicReview extends BaseEntity {
    @Column(nullable = false) private String schoolId;
    private String reviewDate;
    private String cycle;
    private String facilitator;
    private int attendees;
    @Column(columnDefinition = "TEXT") private String highlights;
    @Column(columnDefinition = "TEXT") private String decisions;
}
