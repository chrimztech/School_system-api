package com.srms.api.modules.payroll.repository;

import com.srms.api.modules.payroll.entity.PayrollRun;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface PayrollRunRepository extends JpaRepository<PayrollRun, String> {
    List<PayrollRun> findBySchoolIdOrderByYearDescMonthDesc(String schoolId);
    Optional<PayrollRun> findBySchoolIdAndYearAndMonth(String schoolId, int year, int month);
}
