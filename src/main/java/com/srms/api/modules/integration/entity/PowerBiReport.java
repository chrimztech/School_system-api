package com.srms.api.modules.integration.entity;

import com.srms.api.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * One embeddable/published Power BI report — a school (or the platform, at PLATFORM scope) may
 * publish more than one report (e.g. a fee-collection dashboard and a separate attendance
 * dashboard), each with its own workspace/report/dataset IDs, so these live in their own table
 * rather than as a single set of fields on IntegrationConfig. The Azure AD credentials
 * (tenant/client ID/secret/certificate) that authenticate to Power BI are shared across every
 * report for the same school and stay on the parent IntegrationConfig row (code "powerbi").
 */
@Entity
@Table(name = "power_bi_reports", indexes = {
        @Index(name = "idx_power_bi_reports_scope", columnList = "scope_type, school_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PowerBiReport extends BaseEntity {
    @jakarta.persistence.Enumerated(jakarta.persistence.EnumType.STRING)
    @Column(name = "scope_type", nullable = false)
    private IntegrationConfig.ScopeType scopeType;

    @Column(name = "school_id", nullable = false)
    private String schoolId;

    @Column(name = "workspace_id", nullable = false)
    private String workspaceId;

    @Column(name = "report_id", nullable = false)
    private String reportId;

    @Column(name = "dataset_id", nullable = false)
    private String datasetId;

    @Column(name = "capacity_id")
    private String capacityId;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    /** "embedded" | "published" */
    private String reportType;

    @Column(name = "row_level_security_role")
    private String rowLevelSecurityRole;

    /** "manual" | "daily" | "hourly" */
    @Column(name = "refresh_schedule")
    private String refreshSchedule;

    @Column(name = "last_refresh_at")
    private LocalDateTime lastRefreshAt;

    private Boolean enabled;
}
