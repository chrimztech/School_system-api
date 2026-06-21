package com.srms.api.modules.auth.entity;

import com.srms.api.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "app_users")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AppUser extends BaseEntity {
    @Column(nullable = false, unique = true)
    private String email;
    @Column(nullable = false)
    private String passwordHash;
    @Column(nullable = false)
    private String name;
    private String initials;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;
    private String schoolId; // null for SUPER_ADMIN
    private String phone;
    private boolean active = true;
    public enum UserRole { SUPER_ADMIN, SCHOOL_ADMIN, TEACHER, FINANCE, PARENT }
}
