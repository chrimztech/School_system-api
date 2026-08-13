package com.srms.api.modules.auth.entity;

import com.srms.api.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "app_users", indexes = @Index(name = "idx_app_users_school_id", columnList = "school_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AppUser extends BaseEntity {
    @Column(unique = true)
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
    @Column(unique = true)
    private String phone;
    @Builder.Default private boolean active = true;
    @Builder.Default private boolean notifyEmail = true;
    @Builder.Default private boolean notifySms = false;
    // Set whenever an admin (or onboarding) provisions/resets this account's password, since
    // that password is known to someone other than the account owner. Cleared once the user
    // successfully calls changePassword themselves. See AuthService.
    @Builder.Default
    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean mustChangePassword = false;
    public enum UserRole { SUPER_ADMIN, SCHOOL_ADMIN, TEACHER, HOD, FINANCE, PARENT, PRINCIPAL, DEPUTY_HEAD, CAREER_GUIDANCE }
}
