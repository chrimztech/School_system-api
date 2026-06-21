package com.srms.api.modules.platform.entity;

import com.srms.api.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Lob;
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

    @Lob
    @Column(columnDefinition = "TEXT")
    private String plansJson;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String addOnsJson;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String promotionsJson;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String supportTicketsJson;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String supportSettingsJson;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String approvalItemsJson;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String approvalPoliciesJson;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String statusIncidentsJson;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String maintenanceWindowsJson;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String statusSettingsJson;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String tenantHandoffsJson;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String tenantSuccessOverridesJson;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String tenantLifecycleOverridesJson;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String partnersJson;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String partnerDealsJson;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String contractsJson;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String revenueCasesJson;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String dataRequestsJson;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String exportJobsJson;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String retentionRulesJson;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String residencySettingsJson;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String rolloutsJson;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String platformSecurityJson;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String platformCommunicationsJson;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String platformDefaultsJson;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String developerApiKeysJson;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String developerWebhooksJson;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String developerSandboxesJson;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String platformAuditEventsJson;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String servicesJson;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String queuesJson;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String opsIncidentsJson;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String releasesJson;
}
