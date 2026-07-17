package com.srms.api.modules.auth.entity;

import com.srms.api.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "custom_role_permissions", indexes = @Index(name = "idx_custom_role_permissions_school_id", columnList = "school_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CustomRolePermission extends BaseEntity {
    @Column(name = "school_id", nullable = false)
    private String schoolId;

    @Column(name = "role_name", nullable = false)
    private String roleName;

    @Column(nullable = false)
    private String module;

    /** "full" | "read" | "none" */
    @Column(nullable = false)
    private String access;
}
