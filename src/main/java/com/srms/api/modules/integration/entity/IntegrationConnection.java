package com.srms.api.modules.integration.entity;

import com.srms.api.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Index;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "integration_connections", indexes = @Index(name = "idx_integration_connections_school_id", columnList = "school_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IntegrationConnection extends BaseEntity {
    @Column(nullable = false)
    private String schoolId;

    @Column(nullable = false)
    private String code;

    @Column(nullable = false)
    private String name;

    private String category;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Builder.Default
    @Column(columnDefinition = "boolean default false")
    private Boolean connected = false;

    private String status;
    private String owner;
    private String webhook;
}
