package com.srms.api.modules.audit.entity;

import com.srms.api.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name = "audit_events") @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AuditEvent extends BaseEntity {
    @Column(nullable = false) private String schoolId;
    @Column(nullable = false) private String actor;
    private String role;
    @Column(nullable = false) private String action;
    private String target;
    @Builder.Default private String severity = "info"; // info, warning, success
}
