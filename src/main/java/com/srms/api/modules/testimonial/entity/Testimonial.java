package com.srms.api.modules.testimonial.entity;

import com.srms.api.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "testimonials")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Testimonial extends BaseEntity {
    @Column(nullable = false)
    private String authorName;

    private String authorRole;
    private String schoolName;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String quote;

    @Column(nullable = false)
    private int rating;

    @Column(nullable = false)
    private boolean approved;
}
