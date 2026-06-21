package com.srms.api.modules.reporting.service;

import com.srms.api.modules.reporting.entity.SavedReport;
import com.srms.api.modules.reporting.repository.SavedReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ReportingService {
    private final SavedReportRepository savedReportRepository;

    public List<SavedReport> list(String schoolId) {
        return savedReportRepository.findBySchoolIdOrderByCreatedAtDesc(schoolId);
    }

    public SavedReport create(String schoolId, SavedReport report) {
        report.setSchoolId(schoolId);
        if (report.getStatus() == null) report.setStatus("Draft");
        return savedReportRepository.save(report);
    }
}
