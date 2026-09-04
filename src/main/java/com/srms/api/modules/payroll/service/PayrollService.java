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

    // Zambian PAYE brackets (2024+, monthly, ZMW) and NAPSA's statutory monthly cap — must match
    // the frontend's own PAYE_BANDS/NAPSA_CAP in payroll.tsx exactly, since that page's "Payslip
    // preview" tab shows a client-side estimate using this same math before a run is processed;
    // if these drift apart, the preview a school sees stops matching what actually gets withheld.
    private record PayeBand(BigDecimal upTo, BigDecimal rate) {}
    private static final List<PayeBand> PAYE_BANDS = List.of(
            new PayeBand(new BigDecimal("5100"), BigDecimal.ZERO),
            new PayeBand(new BigDecimal("7100"), new BigDecimal("0.20")),
            new PayeBand(new BigDecimal("9200"), new BigDecimal("0.30")),
            new PayeBand(null, new BigDecimal("0.37")) // null upTo = no ceiling
    );
    private static final BigDecimal NAPSA_RATE = new BigDecimal("0.05");
    private static final BigDecimal NAPSA_CAP = new BigDecimal("1342.40");
    private static final BigDecimal NHIMA_RATE = new BigDecimal("0.01");

    private static BigDecimal calcPaye(BigDecimal taxable) {
        BigDecimal tax = BigDecimal.ZERO;
        BigDecimal prev = BigDecimal.ZERO;
        for (PayeBand band : PAYE_BANDS) {
            if (taxable.compareTo(prev) <= 0) break;
            BigDecimal ceiling = band.upTo() == null ? taxable : band.upTo();
            BigDecimal top = taxable.min(ceiling);
            BigDecimal slice = top.subtract(prev);
            if (slice.signum() > 0) tax = tax.add(slice.multiply(band.rate()));
            prev = ceiling;
        }
        return tax.setScale(2, RoundingMode.HALF_UP);
    }

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
            BigDecimal napsa = gross.multiply(NAPSA_RATE).min(NAPSA_CAP).setScale(2, RoundingMode.HALF_UP);
            BigDecimal nhima = gross.multiply(NHIMA_RATE).setScale(2, RoundingMode.HALF_UP);
            BigDecimal taxable = gross.subtract(napsa); // NAPSA is pre-tax deductible
            BigDecimal paye = calcPaye(taxable);
            BigDecimal net = gross.subtract(napsa).subtract(paye).subtract(nhima);
            PayslipEntry slip = new PayslipEntry();
            slip.setSchoolId(schoolId); slip.setPayrollRunId(runId); slip.setStaffId(s.getId()); slip.setStaffName(s.getName()); slip.setPosition(s.getPosition()); slip.setGrossSalary(gross); slip.setNapsa(napsa); slip.setPaye(paye); slip.setNhima(nhima); slip.setOtherDeductions(BigDecimal.ZERO); slip.setNetSalary(net);
            slipRepo.save(slip);
            totalGross = totalGross.add(gross);
            totalNet = totalNet.add(net);
        }
        run.setTotalGross(totalGross); run.setTotalNet(totalNet); run.setStaffCount(staff.size()); run.setStatus("PROCESSED");
        return runRepo.save(run);
    }
}
