package com.srms.api.modules.platform.entity;

import com.srms.api.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "platform_workspace")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlatformWorkspace extends BaseEntity {
    @Column(nullable = false, unique = true)
    private String workspaceKey;

    @Column(columnDefinition = "TEXT")
    private String plansJson;

    @Column(columnDefinition = "TEXT")
    private String addOnsJson;

    @Column(columnDefinition = "TEXT")
    private String promotionsJson;

    @Column(columnDefinition = "TEXT")
    private String supportTicketsJson;

    @Column(columnDefinition = "TEXT")
    private String supportSettingsJson;

    @Column(columnDefinition = "TEXT")
    private String approvalItemsJson;

    @Column(columnDefinition = "TEXT")
    private String approvalPoliciesJson;

    @Column(columnDefinition = "TEXT")
    private String statusIncidentsJson;

    @Column(columnDefinition = "TEXT")
    private String maintenanceWindowsJson;

    @Column(columnDefinition = "TEXT")
    private String statusSettingsJson;

    @Column(columnDefinition = "TEXT")
    private String tenantHandoffsJson;

    @Column(columnDefinition = "TEXT")
    private String tenantSuccessOverridesJson;

    @Column(columnDefinition = "TEXT")
    private String tenantLifecycleOverridesJson;

    @Column(columnDefinition = "TEXT")
    private String partnersJson;

    @Column(columnDefinition = "TEXT")
    private String partnerDealsJson;

    @Column(columnDefinition = "TEXT")
    private String contractsJson;

    @Column(columnDefinition = "TEXT")
    private String revenueCasesJson;

    @Column(columnDefinition = "TEXT")
    private String dataRequestsJson;

    @Column(columnDefinition = "TEXT")
    private String exportJobsJson;

    @Column(columnDefinition = "TEXT")
    private String retentionRulesJson;

    @Column(columnDefinition = "TEXT")
    private String residencySettingsJson;

    @Column(columnDefinition = "TEXT")
    private String rolloutsJson;

    @Column(columnDefinition = "TEXT")
    private String platformSecurityJson;

    @Column(columnDefinition = "TEXT")
    private String platformCommunicationsJson;

    @Column(columnDefinition = "TEXT")
    private String platformDefaultsJson;

    @Column(columnDefinition = "TEXT")
    private String developerApiKeysJson;

    @Column(columnDefinition = "TEXT")
    private String developerWebhooksJson;

    @Column(columnDefinition = "TEXT")
    private String developerSandboxesJson;

    @Column(columnDefinition = "TEXT")
    private String platformAuditEventsJson;

    @Column(columnDefinition = "TEXT")
    private String servicesJson;

    @Column(columnDefinition = "TEXT")
    private String queuesJson;

    @Column(columnDefinition = "TEXT")
    private String opsIncidentsJson;

    @Column(columnDefinition = "TEXT")
    private String releasesJson;
}
