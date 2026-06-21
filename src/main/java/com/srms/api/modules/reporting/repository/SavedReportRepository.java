package com.srms.api.modules.reporting.repository;

import com.srms.api.modules.reporting.entity.SavedReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SavedReportRepository extends JpaRepository<SavedReport, String> {
    List<SavedReport> findBySchoolIdOrderByCreatedAtDesc(String schoolId);
}
