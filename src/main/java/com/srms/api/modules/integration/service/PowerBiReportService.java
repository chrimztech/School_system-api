package com.srms.api.modules.integration.service;

import com.srms.api.exception.BusinessException;
import com.srms.api.modules.integration.dto.PowerBiReportDto;
import com.srms.api.modules.integration.entity.IntegrationConfig;
import com.srms.api.modules.integration.entity.PowerBiReport;
import com.srms.api.modules.integration.repository.PowerBiReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** CRUD for the Power BI reports a school (or the platform) has published — see PowerBiReport's
 * javadoc for why these live in their own table rather than as fields on IntegrationConfig. */
@Service
@RequiredArgsConstructor
@Transactional
public class PowerBiReportService {
    private final PowerBiReportRepository repository;

    public List<PowerBiReportDto> list(IntegrationConfig.ScopeType scopeType, String schoolId) {
        return repository.findByScopeTypeAndSchoolIdOrderByDisplayNameAsc(scopeType, schoolId).stream()
                .map(PowerBiReportDto::from).toList();
    }

    public PowerBiReportDto create(IntegrationConfig.ScopeType scopeType, String schoolId, PowerBiReportDto dto) {
        requireFields(dto);
        PowerBiReport report = PowerBiReport.builder()
                .scopeType(scopeType).schoolId(schoolId)
                .workspaceId(dto.workspaceId()).reportId(dto.reportId()).datasetId(dto.datasetId())
                .capacityId(dto.capacityId()).displayName(dto.displayName()).reportType(dto.reportType())
                .rowLevelSecurityRole(dto.rowLevelSecurityRole()).refreshSchedule(dto.refreshSchedule())
                .enabled(dto.enabled() == null || dto.enabled())
                .build();
        return PowerBiReportDto.from(repository.save(report));
    }

    public PowerBiReportDto update(IntegrationConfig.ScopeType scopeType, String schoolId, String id, PowerBiReportDto dto) {
        PowerBiReport report = repository.findByIdAndScopeTypeAndSchoolId(id, scopeType, schoolId)
                .orElseThrow(() -> new BusinessException("Report not found"));
        if (dto.workspaceId() != null) report.setWorkspaceId(dto.workspaceId());
        if (dto.reportId() != null) report.setReportId(dto.reportId());
        if (dto.datasetId() != null) report.setDatasetId(dto.datasetId());
        if (dto.capacityId() != null) report.setCapacityId(dto.capacityId());
        if (dto.displayName() != null) report.setDisplayName(dto.displayName());
        if (dto.reportType() != null) report.setReportType(dto.reportType());
        if (dto.rowLevelSecurityRole() != null) report.setRowLevelSecurityRole(dto.rowLevelSecurityRole());
        if (dto.refreshSchedule() != null) report.setRefreshSchedule(dto.refreshSchedule());
        if (dto.enabled() != null) report.setEnabled(dto.enabled());
        return PowerBiReportDto.from(repository.save(report));
    }

    public void delete(IntegrationConfig.ScopeType scopeType, String schoolId, String id) {
        PowerBiReport report = repository.findByIdAndScopeTypeAndSchoolId(id, scopeType, schoolId)
                .orElseThrow(() -> new BusinessException("Report not found"));
        repository.delete(report);
    }

    private static void requireFields(PowerBiReportDto dto) {
        if (isBlank(dto.workspaceId()) || isBlank(dto.reportId()) || isBlank(dto.datasetId()) || isBlank(dto.displayName())) {
            throw new BusinessException("Workspace ID, Report ID, Dataset ID, and a display name are all required");
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
