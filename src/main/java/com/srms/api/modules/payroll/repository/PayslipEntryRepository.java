package com.srms.api.modules.payroll.repository;

import com.srms.api.modules.payroll.entity.PayslipEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PayslipEntryRepository extends JpaRepository<PayslipEntry, String> {
    List<PayslipEntry> findBySchoolIdAndPayrollRunId(String schoolId, String payrollRunId);
    List<PayslipEntry> findBySchoolIdAndStaffId(String schoolId, String staffId);
}
