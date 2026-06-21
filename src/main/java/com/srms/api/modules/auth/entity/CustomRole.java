package com.srms.api.modules.auth.entity;

import com.srms.api.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "custom_roles",
    uniqueConstraints = @UniqueConstraint(columnNames = {"school_id", "name"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CustomRole extends BaseEntity {
    @Column(name = "school_id", nullable = false)
    private String schoolId;

    @Column(nullable = false)
    private String name;

    private String description;

    @Builder.Default
    private boolean active = true;
}
