package com.srms.api.modules.academic.entity;

import com.srms.api.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "departments",
    uniqueConstraints = @UniqueConstraint(columnNames = {"school_id", "name"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Department extends BaseEntity {
    @Column(name = "school_id", nullable = false) private String schoolId;
    @Column(nullable = false)                      private String name;
    private String code;
    private String description;
    private String head;
    @Builder.Default private boolean active = true;
}
