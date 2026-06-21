package com.srms.api.modules.payroll.service;

import com.srms.api.modules.hr.entity.StaffRecord;
import com.srms.api.modules.hr.repository.StaffRepository;
import com.srms.api.modules.payroll.entity.PayrollRun;
import com.srms.api.modules.payroll.entity.PayslipEntry;
import com.srms.api.modules.payroll.repository.PayrollRunRepository;
import com.srms.api.modules.payroll.repository.PayslipEntryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Service @RequiredArgsConstructor @Transactional
public class PayrollService {
    private final PayrollRunRepository runRepo;
    private final PayslipEntryRepository slipRepo;
    private final StaffRepository staffRepository;

    public List<PayrollRun> getRuns(String schoolId) { return runRepo.findBySchoolIdOrderByYearDescMonthDesc(schoolId); }

    public PayrollRun createRun(String schoolId, PayrollRun run) {
        run.setSchoolId(schoolId);
        run.setStatus("DRAFT");
        if (run.getRunDate() == null) run.setRunDate(LocalDate.now());
        return runRepo.save(run);
    }

    public List<PayslipEntry> getPayslips(String schoolId, String runId) { return slipRepo.findBySchoolIdAndPayrollRunId(schoolId, runId); }

    public PayrollRun processRun(String schoolId, String runId) {
        PayrollRun run = runRepo.findById(runId).filter(r -> r.getSchoolId().equals(schoolId)).orElseThrow();
        List<StaffRecord> staff = staffRepository.findBySchoolIdAndStatus(schoolId, StaffRecord.StaffStatus.ACTIVE);
        slipRepo.deleteAll(slipRepo.findBySchoolIdAndPayrollRunId(schoolId, runId));

        BigDecimal totalGross = BigDecimal.ZERO;
        BigDecimal totalNet = BigDecimal.ZERO;

        for (StaffRecord s : staff) {
            BigDecimal gross = s.getSalary() != null ? s.getSalary() : BigDecimal.ZERO;
            BigDecimal napsa = gross.multiply(new BigDecimal("0.05")).setScale(2, RoundingMode.HALF_UP);
            BigDecimal paye = gross.multiply(new BigDecimal("0.25")).setScale(2, RoundingMode.HALF_UP);
            BigDecimal nhima = gross.multiply(new BigDecimal("0.01")).setScale(2, RoundingMode.HALF_UP);
            BigDecimal net = gross.subtract(napsa).subtract(paye).subtract(nhima);
            PayslipEntry slip = new PayslipEntry();
            slip.setSchoolId(schoolId); slip.setPayrollRunId(runId); slip.setStaffId(s.getId()); slip.setStaffName(s.getUserId()); slip.setPosition(s.getPosition()); slip.setGrossSalary(gross); slip.setNapsa(napsa); slip.setPaye(paye); slip.setNhima(nhima); slip.setOtherDeductions(BigDecimal.ZERO); slip.setNetSalary(net);
            slipRepo.save(slip);
            totalGross = totalGross.add(gross);
            totalNet = totalNet.add(net);
        }
        run.setTotalGross(totalGross); run.setTotalNet(totalNet); run.setStaffCount(staff.size()); run.setStatus("PROCESSED");
        return runRepo.save(run);
    }
}
