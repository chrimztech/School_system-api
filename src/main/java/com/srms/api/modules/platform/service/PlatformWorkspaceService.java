package com.srms.api.modules.platform.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.srms.api.modules.platform.dto.PlatformWorkspaceDto;
import com.srms.api.modules.platform.dto.SupportTicketRequest;
import com.srms.api.modules.platform.entity.PlatformWorkspace;
import com.srms.api.modules.platform.repository.PlatformWorkspaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Transactional
public class PlatformWorkspaceService {
    private static final String DEFAULT_WORKSPACE_KEY = "default";
    private static final TypeReference<List<Map<String, Object>>> LIST_OF_MAPS = new TypeReference<>() {};
    private static final TypeReference<Map<String, Object>> MAP_OF_OBJECTS = new TypeReference<>() {};
    private static final TypeReference<Map<String, Boolean>> MAP_OF_BOOLEANS = new TypeReference<>() {};

    private final PlatformWorkspaceRepository platformWorkspaceRepository;
    private final ObjectMapper objectMapper;

    public PlatformWorkspaceDto getWorkspace() {
        PlatformWorkspace workspace = platformWorkspaceRepository.findByWorkspaceKey(DEFAULT_WORKSPACE_KEY)
                .orElseGet(this::createDefaultWorkspace);

        if (shouldClearLegacySeedPayloads(workspace)) {
            clearWorkspacePayloads(workspace);
            workspace = platformWorkspaceRepository.save(workspace);
        }

        return toDto(workspace);
    }

    public PlatformWorkspaceDto updateWorkspace(PlatformWorkspaceDto dto) {
        PlatformWorkspace workspace = platformWorkspaceRepository.findByWorkspaceKey(DEFAULT_WORKSPACE_KEY)
                .orElseGet(this::createDefaultWorkspace);
        merge(workspace, dto);
        return toDto(platformWorkspaceRepository.save(workspace));
    }

    /** Narrow, safe entry point for any authenticated school user to raise a support ticket —
     * unlike getWorkspace()/updateWorkspace() this never exposes or accepts the full platform
     * blob, it only ever appends one ticket to the existing list. */
    public void submitSupportTicket(SupportTicketRequest request) {
        PlatformWorkspace workspace = platformWorkspaceRepository.findByWorkspaceKey(DEFAULT_WORKSPACE_KEY)
                .orElseGet(this::createDefaultWorkspace);
        List<Map<String, Object>> tickets = readJson(workspace.getSupportTicketsJson(), LIST_OF_MAPS, new ArrayList<>());
        Map<String, Object> ticket = new LinkedHashMap<>();
        ticket.put("id", "SUP-" + ThreadLocalRandom.current().nextInt(100000, 999999));
        ticket.put("tenantId", "");
        ticket.put("tenantName", request.getTenantName());
        ticket.put("subject", request.getSubject());
        ticket.put("category", request.getCategory() != null ? request.getCategory() : "General");
        ticket.put("priority", "Medium");
        ticket.put("status", "New");
        ticket.put("owner", "Unassigned");
        ticket.put("slaHours", 24);
        ticket.put("ageHours", 0);
        ticket.put("article", "");
        ticket.put("description", request.getMessage());
        ticket.put("reporterName", request.getReporterName());
        ticket.put("reporterEmail", request.getReporterEmail());
        ticket.put("submittedAt", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        List<Map<String, Object>> next = new ArrayList<>();
        next.add(ticket);
        next.addAll(tickets);
        workspace.setSupportTicketsJson(writeJson(next));
        platformWorkspaceRepository.save(workspace);
    }

    private PlatformWorkspace createDefaultWorkspace() {
        PlatformWorkspace workspace = new PlatformWorkspace();
        workspace.setWorkspaceKey(DEFAULT_WORKSPACE_KEY);
        return platformWorkspaceRepository.save(workspace);
    }

    private boolean shouldClearLegacySeedPayloads(PlatformWorkspace workspace) {
        if (!hasPersistedPayload(workspace) || workspace.getCreatedAt() == null || workspace.getUpdatedAt() == null) {
            return false;
        }
        return !workspace.getUpdatedAt().isAfter(workspace.getCreatedAt());
    }

    private boolean hasPersistedPayload(PlatformWorkspace workspace) {
        return Stream.of(
                workspace.getPlansJson(),
                workspace.getAddOnsJson(),
                workspace.getPromotionsJson(),
                workspace.getSupportTicketsJson(),
                workspace.getSupportSettingsJson(),
                workspace.getApprovalItemsJson(),
                workspace.getApprovalPoliciesJson(),
                workspace.getStatusIncidentsJson(),
                workspace.getMaintenanceWindowsJson(),
                workspace.getStatusSettingsJson(),
                workspace.getTenantHandoffsJson(),
                workspace.getTenantSuccessOverridesJson(),
                workspace.getTenantLifecycleOverridesJson(),
                workspace.getPartnersJson(),
                workspace.getPartnerDealsJson(),
                workspace.getContractsJson(),
                workspace.getRevenueCasesJson(),
                workspace.getDataRequestsJson(),
                workspace.getExportJobsJson(),
                workspace.getRetentionRulesJson(),
                workspace.getResidencySettingsJson(),
                workspace.getRolloutsJson(),
                workspace.getPlatformSecurityJson(),
                workspace.getPlatformCommunicationsJson(),
                workspace.getPlatformDefaultsJson(),
                workspace.getDeveloperApiKeysJson(),
                workspace.getDeveloperWebhooksJson(),
                workspace.getDeveloperSandboxesJson(),
                workspace.getPlatformAuditEventsJson(),
                workspace.getServicesJson(),
                workspace.getQueuesJson(),
                workspace.getOpsIncidentsJson(),
                workspace.getReleasesJson()
        ).anyMatch(json -> json != null && !json.isBlank());
    }

    private void clearWorkspacePayloads(PlatformWorkspace workspace) {
        workspace.setPlansJson(null);
        workspace.setAddOnsJson(null);
        workspace.setPromotionsJson(null);
        workspace.setSupportTicketsJson(null);
        workspace.setSupportSettingsJson(null);
        workspace.setApprovalItemsJson(null);
        workspace.setApprovalPoliciesJson(null);
        workspace.setStatusIncidentsJson(null);
        workspace.setMaintenanceWindowsJson(null);
        workspace.setStatusSettingsJson(null);
        workspace.setTenantHandoffsJson(null);
        workspace.setTenantSuccessOverridesJson(null);
        workspace.setTenantLifecycleOverridesJson(null);
        workspace.setPartnersJson(null);
        workspace.setPartnerDealsJson(null);
        workspace.setContractsJson(null);
        workspace.setRevenueCasesJson(null);
        workspace.setDataRequestsJson(null);
        workspace.setExportJobsJson(null);
        workspace.setRetentionRulesJson(null);
        workspace.setResidencySettingsJson(null);
        workspace.setRolloutsJson(null);
        workspace.setPlatformSecurityJson(null);
        workspace.setPlatformCommunicationsJson(null);
        workspace.setPlatformDefaultsJson(null);
        workspace.setDeveloperApiKeysJson(null);
        workspace.setDeveloperWebhooksJson(null);
        workspace.setDeveloperSandboxesJson(null);
        workspace.setPlatformAuditEventsJson(null);
        workspace.setServicesJson(null);
        workspace.setQueuesJson(null);
        workspace.setOpsIncidentsJson(null);
        workspace.setReleasesJson(null);
    }

    private PlatformWorkspaceDto toDto(PlatformWorkspace workspace) {
        return PlatformWorkspaceDto.builder()
                .plans(readJson(workspace.getPlansJson(), LIST_OF_MAPS, new ArrayList<>()))
                .addOns(readJson(workspace.getAddOnsJson(), LIST_OF_MAPS, new ArrayList<>()))
                .promotions(readJson(workspace.getPromotionsJson(), LIST_OF_MAPS, new ArrayList<>()))
                .supportTickets(readJson(workspace.getSupportTicketsJson(), LIST_OF_MAPS, new ArrayList<>()))
                .supportSettings(readJson(workspace.getSupportSettingsJson(), MAP_OF_OBJECTS, new LinkedHashMap<>()))
                .approvalItems(readJson(workspace.getApprovalItemsJson(), LIST_OF_MAPS, new ArrayList<>()))
                .approvalPolicies(readJson(workspace.getApprovalPoliciesJson(), MAP_OF_BOOLEANS, new LinkedHashMap<>()))
                .statusIncidents(readJson(workspace.getStatusIncidentsJson(), LIST_OF_MAPS, new ArrayList<>()))
                .maintenanceWindows(readJson(workspace.getMaintenanceWindowsJson(), LIST_OF_MAPS, new ArrayList<>()))
                .statusSettings(readJson(workspace.getStatusSettingsJson(), MAP_OF_OBJECTS, new LinkedHashMap<>()))
                .tenantHandoffs(readJson(workspace.getTenantHandoffsJson(), LIST_OF_MAPS, new ArrayList<>()))
                .tenantSuccessOverrides(readJson(workspace.getTenantSuccessOverridesJson(), MAP_OF_OBJECTS, new LinkedHashMap<>()))
                .tenantLifecycleOverrides(readJson(workspace.getTenantLifecycleOverridesJson(), MAP_OF_OBJECTS, new LinkedHashMap<>()))
                .partners(readJson(workspace.getPartnersJson(), LIST_OF_MAPS, new ArrayList<>()))
                .partnerDeals(readJson(workspace.getPartnerDealsJson(), LIST_OF_MAPS, new ArrayList<>()))
                .contracts(readJson(workspace.getContractsJson(), LIST_OF_MAPS, new ArrayList<>()))
                .revenueCases(readJson(workspace.getRevenueCasesJson(), LIST_OF_MAPS, new ArrayList<>()))
                .dataRequests(readJson(workspace.getDataRequestsJson(), LIST_OF_MAPS, new ArrayList<>()))
                .exportJobs(readJson(workspace.getExportJobsJson(), LIST_OF_MAPS, new ArrayList<>()))
                .retentionRules(readJson(workspace.getRetentionRulesJson(), LIST_OF_MAPS, new ArrayList<>()))
                .residencySettings(readJson(workspace.getResidencySettingsJson(), MAP_OF_OBJECTS, new LinkedHashMap<>()))
                .rollouts(readJson(workspace.getRolloutsJson(), LIST_OF_MAPS, new ArrayList<>()))
                .platformSecurity(readJson(workspace.getPlatformSecurityJson(), MAP_OF_OBJECTS, new LinkedHashMap<>()))
                .platformCommunications(readJson(workspace.getPlatformCommunicationsJson(), MAP_OF_OBJECTS, new LinkedHashMap<>()))
                .platformDefaults(readJson(workspace.getPlatformDefaultsJson(), MAP_OF_OBJECTS, new LinkedHashMap<>()))
                .developerApiKeys(readJson(workspace.getDeveloperApiKeysJson(), LIST_OF_MAPS, new ArrayList<>()))
                .developerWebhooks(readJson(workspace.getDeveloperWebhooksJson(), LIST_OF_MAPS, new ArrayList<>()))
                .developerSandboxes(readJson(workspace.getDeveloperSandboxesJson(), LIST_OF_MAPS, new ArrayList<>()))
                .platformAuditEvents(readJson(workspace.getPlatformAuditEventsJson(), LIST_OF_MAPS, new ArrayList<>()))
                .services(readJson(workspace.getServicesJson(), LIST_OF_MAPS, new ArrayList<>()))
                .queues(readJson(workspace.getQueuesJson(), LIST_OF_MAPS, new ArrayList<>()))
                .opsIncidents(readJson(workspace.getOpsIncidentsJson(), LIST_OF_MAPS, new ArrayList<>()))
                .releases(readJson(workspace.getReleasesJson(), LIST_OF_MAPS, new ArrayList<>()))
                .build();
    }

    private void merge(PlatformWorkspace workspace, PlatformWorkspaceDto dto) {
        if (dto.getPlans() != null) workspace.setPlansJson(writeJson(dto.getPlans()));
        if (dto.getAddOns() != null) workspace.setAddOnsJson(writeJson(dto.getAddOns()));
        if (dto.getPromotions() != null) workspace.setPromotionsJson(writeJson(dto.getPromotions()));
        if (dto.getSupportTickets() != null) workspace.setSupportTicketsJson(writeJson(dto.getSupportTickets()));
        if (dto.getSupportSettings() != null) workspace.setSupportSettingsJson(writeJson(dto.getSupportSettings()));
        if (dto.getApprovalItems() != null) workspace.setApprovalItemsJson(writeJson(dto.getApprovalItems()));
        if (dto.getApprovalPolicies() != null) workspace.setApprovalPoliciesJson(writeJson(dto.getApprovalPolicies()));
        if (dto.getStatusIncidents() != null) workspace.setStatusIncidentsJson(writeJson(dto.getStatusIncidents()));
        if (dto.getMaintenanceWindows() != null) workspace.setMaintenanceWindowsJson(writeJson(dto.getMaintenanceWindows()));
        if (dto.getStatusSettings() != null) workspace.setStatusSettingsJson(writeJson(dto.getStatusSettings()));
        if (dto.getTenantHandoffs() != null) workspace.setTenantHandoffsJson(writeJson(dto.getTenantHandoffs()));
        if (dto.getTenantSuccessOverrides() != null) workspace.setTenantSuccessOverridesJson(writeJson(dto.getTenantSuccessOverrides()));
        if (dto.getTenantLifecycleOverrides() != null) workspace.setTenantLifecycleOverridesJson(writeJson(dto.getTenantLifecycleOverrides()));
        if (dto.getPartners() != null) workspace.setPartnersJson(writeJson(dto.getPartners()));
        if (dto.getPartnerDeals() != null) workspace.setPartnerDealsJson(writeJson(dto.getPartnerDeals()));
        if (dto.getContracts() != null) workspace.setContractsJson(writeJson(dto.getContracts()));
        if (dto.getRevenueCases() != null) workspace.setRevenueCasesJson(writeJson(dto.getRevenueCases()));
        if (dto.getDataRequests() != null) workspace.setDataRequestsJson(writeJson(dto.getDataRequests()));
        if (dto.getExportJobs() != null) workspace.setExportJobsJson(writeJson(dto.getExportJobs()));
        if (dto.getRetentionRules() != null) workspace.setRetentionRulesJson(writeJson(dto.getRetentionRules()));
        if (dto.getResidencySettings() != null) workspace.setResidencySettingsJson(writeJson(dto.getResidencySettings()));
        if (dto.getRollouts() != null) workspace.setRolloutsJson(writeJson(dto.getRollouts()));
        if (dto.getPlatformSecurity() != null) workspace.setPlatformSecurityJson(writeJson(dto.getPlatformSecurity()));
        if (dto.getPlatformCommunications() != null) workspace.setPlatformCommunicationsJson(writeJson(dto.getPlatformCommunications()));
        if (dto.getPlatformDefaults() != null) workspace.setPlatformDefaultsJson(writeJson(dto.getPlatformDefaults()));
        if (dto.getDeveloperApiKeys() != null) workspace.setDeveloperApiKeysJson(writeJson(dto.getDeveloperApiKeys()));
        if (dto.getDeveloperWebhooks() != null) workspace.setDeveloperWebhooksJson(writeJson(dto.getDeveloperWebhooks()));
        if (dto.getDeveloperSandboxes() != null) workspace.setDeveloperSandboxesJson(writeJson(dto.getDeveloperSandboxes()));
        if (dto.getPlatformAuditEvents() != null) workspace.setPlatformAuditEventsJson(writeJson(dto.getPlatformAuditEvents()));
        if (dto.getServices() != null) workspace.setServicesJson(writeJson(dto.getServices()));
        if (dto.getQueues() != null) workspace.setQueuesJson(writeJson(dto.getQueues()));
        if (dto.getOpsIncidents() != null) workspace.setOpsIncidentsJson(writeJson(dto.getOpsIncidents()));
        if (dto.getReleases() != null) workspace.setReleasesJson(writeJson(dto.getReleases()));
    }
    private <T> T readJson(String json, TypeReference<T> typeReference, T fallback) {
        if (json == null || json.isBlank()) {
            return fallback;
        }
        try {
            return objectMapper.readValue(json, typeReference);
        } catch (JsonProcessingException exception) {
            return fallback;
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to write platform workspace payload", exception);
        }
    }
}

