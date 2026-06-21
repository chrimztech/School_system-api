package com.srms.api.modules.platform.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlatformWorkspaceDto {
    private List<Map<String, Object>> plans;
    private List<Map<String, Object>> addOns;
    private List<Map<String, Object>> promotions;
    private List<Map<String, Object>> supportTickets;
    private Map<String, Object> supportSettings;
    private List<Map<String, Object>> approvalItems;
    private Map<String, Boolean> approvalPolicies;
    private List<Map<String, Object>> statusIncidents;
    private List<Map<String, Object>> maintenanceWindows;
    private Map<String, Object> statusSettings;
    private List<Map<String, Object>> tenantHandoffs;
    private Map<String, Object> tenantSuccessOverrides;
    private Map<String, Object> tenantLifecycleOverrides;
    private List<Map<String, Object>> partners;
    private List<Map<String, Object>> partnerDeals;
    private List<Map<String, Object>> contracts;
    private List<Map<String, Object>> revenueCases;
    private List<Map<String, Object>> dataRequests;
    private List<Map<String, Object>> exportJobs;
    private List<Map<String, Object>> retentionRules;
    private Map<String, Object> residencySettings;
    private List<Map<String, Object>> rollouts;
    private Map<String, Object> platformSecurity;
    private Map<String, Object> platformCommunications;
    private Map<String, Object> platformDefaults;
    private List<Map<String, Object>> developerApiKeys;
    private List<Map<String, Object>> developerWebhooks;
    private List<Map<String, Object>> developerSandboxes;
    private List<Map<String, Object>> platformAuditEvents;
    private List<Map<String, Object>> services;
    private List<Map<String, Object>> queues;
    private List<Map<String, Object>> opsIncidents;
    private List<Map<String, Object>> releases;
}
