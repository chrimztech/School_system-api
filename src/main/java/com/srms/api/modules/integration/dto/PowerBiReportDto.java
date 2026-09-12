package com.srms.api.modules.integration.dto;

import com.srms.api.modules.integration.entity.PowerBiReport;

import java.time.LocalDateTime;

public record PowerBiReportDto(
        String id,
        String workspaceId,
        String reportId,
        String datasetId,
        String capacityId,
        String displayName,
        String reportType,
        String rowLevelSecurityRole,
        String refreshSchedule,
        LocalDateTime lastRefreshAt,
        Boolean enabled
) {
    public static PowerBiReportDto from(PowerBiReport r) {
        return new PowerBiReportDto(r.getId(), r.getWorkspaceId(), r.getReportId(), r.getDatasetId(), r.getCapacityId(),
                r.getDisplayName(), r.getReportType(), r.getRowLevelSecurityRole(), r.getRefreshSchedule(),
                r.getLastRefreshAt(), Boolean.TRUE.equals(r.getEnabled()));
    }
}
