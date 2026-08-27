package com.srms.api.modules.fee.service;
import com.srms.api.modules.fee.entity.FeeBillingRule;
import com.srms.api.modules.fee.entity.FeeDiscountRule;
import com.srms.api.modules.fee.entity.FeeLevy;
import com.srms.api.modules.fee.entity.FeePayment;
import com.srms.api.modules.fee.entity.FeeStructure;
import com.srms.api.modules.fee.repository.FeeBillingRuleRepository;
import com.srms.api.modules.fee.repository.FeeDiscountRuleRepository;
import com.srms.api.modules.fee.repository.FeeLevyRepository;
import com.srms.api.modules.fee.repository.FeePaymentRepository;
import com.srms.api.modules.fee.repository.FeeStructureRepository;
import com.srms.api.modules.student.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service @RequiredArgsConstructor @Transactional
public class FeeService {
    private final FeePaymentRepository paymentRepository;
    private final FeeStructureRepository structureRepository;
    private final FeeLevyRepository levyRepository;
    private final FeeDiscountRuleRepository discountRuleRepository;
    private final FeeBillingRuleRepository billingRuleRepository;
    private final StudentRepository studentRepository;
    public List<FeePayment> getAllPayments(String schoolId) { return paymentRepository.findBySchoolIdOrderByPaymentDateDesc(schoolId); }
    public Page<FeePayment> getAllPaymentsPaged(String schoolId, Pageable pageable) { return paymentRepository.findBySchoolIdOrderByPaymentDateDesc(schoolId, pageable); }
    public List<FeePayment> getStudentPayments(String schoolId, String studentId) { return paymentRepository.findBySchoolIdAndStudentId(schoolId, studentId); }
    public FeePayment recordPayment(String schoolId, FeePayment payment) {
        payment.setSchoolId(schoolId);
        payment.setStatus(FeePayment.PaymentStatus.completed);
        FeePayment saved = paymentRepository.save(payment);
        applyToStudentBalance(saved);
        return saved;
    }

    public void applyToStudentBalance(FeePayment payment) {
        studentRepository.findByIdAndSchoolId(payment.getStudentId(), payment.getSchoolId()).ifPresent(student -> {
            double newBalance = Math.max(0.0, student.getFeeBalance() - payment.getAmount());
            student.setFeeBalance(newBalance);
            studentRepository.save(student);
        });
    }

    /** Undoes a completed payment's effect on the student's balance — used when correcting or
     *  reversing an entry that was already applied, so a fix doesn't leave the balance wrong. */
    private void reverseFromStudentBalance(FeePayment payment) {
        studentRepository.findByIdAndSchoolId(payment.getStudentId(), payment.getSchoolId()).ifPresent(student -> {
            student.setFeeBalance(student.getFeeBalance() + payment.getAmount());
            studentRepository.save(student);
        });
    }

    /** Corrects an already-recorded payment (wrong amount, wrong method, typo in the reference
     *  number, etc.). Reverses the old amount off the student's balance and re-applies the
     *  corrected one, so accounts staff fixing a mis-keyed entry never leaves the balance out
     *  of sync with what was actually collected. */
    public FeePayment updatePayment(String schoolId, String id, FeePayment patch) {
        FeePayment payment = paymentRepository.findById(id).filter(item -> item.getSchoolId().equals(schoolId)).orElseThrow();
        boolean wasCompleted = payment.getStatus() == FeePayment.PaymentStatus.completed;
        if (wasCompleted) reverseFromStudentBalance(payment);

        if (patch.getAmount() != 0) payment.setAmount(patch.getAmount());
        if (patch.getMethod() != null) payment.setMethod(patch.getMethod());
        if (patch.getPaymentDate() != null) payment.setPaymentDate(patch.getPaymentDate());
        if (patch.getReferenceNumber() != null) payment.setReferenceNumber(patch.getReferenceNumber());
        if (patch.getDescription() != null) payment.setDescription(patch.getDescription());
        if (patch.getReceiptNumber() != null) payment.setReceiptNumber(patch.getReceiptNumber());
        if (patch.getFeeCategory() != null) payment.setFeeCategory(patch.getFeeCategory());
        if (patch.getTermPeriod() != null) payment.setTermPeriod(patch.getTermPeriod());

        FeePayment saved = paymentRepository.save(payment);
        if (saved.getStatus() == FeePayment.PaymentStatus.completed) applyToStudentBalance(saved);
        return saved;
    }

    /** A payment is never hard-deleted — a financial record disappearing without a trace is
     *  its own audit problem. "Deleting" marks it reversed (the entity already models this
     *  status) and undoes its effect on the student's balance, keeping the entry visible for
     *  reconciliation instead of erasing what actually happened. */
    public FeePayment reversePayment(String schoolId, String id) {
        FeePayment payment = paymentRepository.findById(id).filter(item -> item.getSchoolId().equals(schoolId)).orElseThrow();
        if (payment.getStatus() == FeePayment.PaymentStatus.completed) reverseFromStudentBalance(payment);
        payment.setStatus(FeePayment.PaymentStatus.reversed);
        return paymentRepository.save(payment);
    }

    public double getTotalCollected(String schoolId) { Double sum = paymentRepository.sumCollected(schoolId); return sum != null ? sum : 0; }

    /**
     * What a student in the given grade owes for the school's current term: the sum of every
     * active FeeStructure covering that grade/term/year (schools commonly split this into
     * separate line items — tuition, boarding, exam registration, etc.) plus every mandatory
     * FeeLevy that applies to that grade. Returns 0 if nothing has been configured yet, rather
     * than blocking admission. A structure/levy scoped to "BOARDING" or "DAY" only counts
     * toward students actually in that boarding status — a day scholar must never be billed
     * a boarding-only fee item, and vice versa.
     */
    public double computeInitialBalance(String schoolId, int grade, int currentTerm, int currentYear, String schoolType, String studentBoardingStatus) {
        double structureFee = structureRepository.findBySchoolIdOrderByAcademicYearDescGradeFromAscTermAsc(schoolId).stream()
                .filter(FeeStructure::isActive)
                .filter(fs -> fs.getAcademicYear() == currentYear)
                .filter(fs -> grade >= fs.getGradeFrom() && grade <= fs.getGradeTo())
                .filter(fs -> termMatches(fs.getTerm(), currentTerm))
                .filter(fs -> boardingStatusMatches(fs.getBoardingStatus(), studentBoardingStatus))
                .mapToDouble(FeeStructure::getTermFee)
                .sum();

        double leviesTotal = levyRepository.findBySchoolIdOrderByCreatedAtDesc(schoolId).stream()
                .filter(levy -> Boolean.TRUE.equals(levy.getMandatory()))
                .filter(levy -> gradeMatches(levy.getGrade(), grade, schoolType))
                .filter(levy -> applicableToMatches(levy.getApplicableTo(), studentBoardingStatus))
                .map(FeeLevy::getAmount)
                .filter(java.util.Objects::nonNull)
                .mapToDouble(java.math.BigDecimal::doubleValue)
                .sum();

        return structureFee + leviesTotal;
    }

    /** null/blank on the structure means "every student regardless of boarding status". */
    private boolean boardingStatusMatches(String structureBoardingStatus, String studentBoardingStatus) {
        if (structureBoardingStatus == null || structureBoardingStatus.isBlank()) return true;
        String student = studentBoardingStatus == null || studentBoardingStatus.isBlank() ? "DAY" : studentBoardingStatus;
        return structureBoardingStatus.equalsIgnoreCase(student);
    }

    /** Levy's free-text "applicableTo" ("All students", "Boarding students", "Day scholars",
     *  "Sport participants", …) — only the boarding-related values are matched against real
     *  student data; the others (no backing student attribute yet) fall through as always
     *  applicable rather than silently never billing anyone, matching prior behavior. */
    private boolean applicableToMatches(String applicableTo, String studentBoardingStatus) {
        if (applicableTo == null || applicableTo.isBlank()) return true;
        String normalized = applicableTo.trim().toLowerCase();
        String student = studentBoardingStatus == null || studentBoardingStatus.isBlank() ? "DAY" : studentBoardingStatus.toUpperCase();
        if (normalized.contains("boarding")) return "BOARDING".equals(student);
        if (normalized.contains("day")) return "DAY".equals(student);
        return true;
    }

    private boolean termMatches(String structureTerm, int currentTerm) {
        if (structureTerm == null || structureTerm.isBlank()) return true;
        String digits = structureTerm.replaceAll("[^0-9]", "");
        if (!digits.isEmpty()) {
            try {
                return Integer.parseInt(digits) == currentTerm;
            } catch (NumberFormatException ignored) {
                // fall through to string comparison
            }
        }
        return structureTerm.trim().equalsIgnoreCase(String.valueOf(currentTerm));
    }

    private static final java.util.regex.Pattern GRADE_LABEL =
            java.util.regex.Pattern.compile("^(Form|Grade)\\s+(\\d+)$", java.util.regex.Pattern.CASE_INSENSITIVE);

    /**
     * Matches a mandatory levy's stored grade label (e.g. "Form 3", "Grade 5", "All forms")
     * against a student's raw numeric grade. Mirrors the frontend's gradeLabelToNumber — a
     * COMBINED/FULL school stores "Form N" as raw grade N+6, so a bare digit-extraction here
     * would silently never match any Form-scoped levy on those school types.
     */
    private boolean gradeMatches(String levyGrade, int grade, String schoolType) {
        if (levyGrade == null || levyGrade.isBlank()) return true;
        String trimmed = levyGrade.trim();
        if (trimmed.toLowerCase().startsWith("all")) return true;
        java.util.regex.Matcher m = GRADE_LABEL.matcher(trimmed);
        if (m.matches()) {
            int n = Integer.parseInt(m.group(2));
            boolean isForm = m.group(1).equalsIgnoreCase("form");
            boolean combined = "COMBINED".equalsIgnoreCase(schoolType) || "FULL".equalsIgnoreCase(schoolType);
            int rawGrade = combined && isForm ? n + 6 : n;
            return rawGrade == grade;
        }
        // Legacy data: a levy grade stored as a bare number
        return trimmed.equals(String.valueOf(grade));
    }
    public List<FeeStructure> getFeeStructures(String schoolId) { return structureRepository.findBySchoolIdOrderByAcademicYearDescGradeFromAscTermAsc(schoolId); }
    public FeeStructure createFeeStructure(String schoolId, FeeStructure fs) { fs.setSchoolId(schoolId); return structureRepository.save(fs); }
    public FeeStructure updateFeeStructure(String schoolId, String id, FeeStructure patch) {
        FeeStructure structure = structureRepository.findById(id).filter(item -> item.getSchoolId().equals(schoolId)).orElseThrow();
        structure.setActive(patch.isActive());
        if (patch.getName() != null) structure.setName(patch.getName());
        if (patch.getDescription() != null) structure.setDescription(patch.getDescription());
        if (patch.getDueDate() != null) structure.setDueDate(patch.getDueDate());
        if (patch.getLatePenaltyAmount() != null) structure.setLatePenaltyAmount(patch.getLatePenaltyAmount());
        if (patch.getPenaltyGraceDays() != null) structure.setPenaltyGraceDays(patch.getPenaltyGraceDays());
        if (patch.getNotes() != null) structure.setNotes(patch.getNotes());
        // Corrections to the actual amount/scope must be editable too — a fee set up with the
        // wrong figure or grade range previously had no way to be fixed short of delete+recreate.
        if (patch.getTermFee() != 0) structure.setTermFee(patch.getTermFee());
        if (patch.getAnnualFee() != 0) structure.setAnnualFee(patch.getAnnualFee());
        if (patch.getGradeFrom() != 0) structure.setGradeFrom(patch.getGradeFrom());
        if (patch.getGradeTo() != 0) structure.setGradeTo(patch.getGradeTo());
        if (patch.getTerm() != null) structure.setTerm(patch.getTerm());
        if (patch.getAcademicYear() != 0) structure.setAcademicYear(patch.getAcademicYear());
        if (patch.getBoardingStatus() != null) structure.setBoardingStatus(patch.getBoardingStatus());
        return structureRepository.save(structure);
    }
    public void deleteFeeStructure(String schoolId, String id) {
        FeeStructure structure = structureRepository.findById(id).filter(item -> item.getSchoolId().equals(schoolId)).orElseThrow();
        structureRepository.delete(structure);
    }

    public List<FeeLevy> getLevies(String schoolId) { return levyRepository.findBySchoolIdOrderByCreatedAtDesc(schoolId); }
    public FeeLevy createLevy(String schoolId, FeeLevy levy) { levy.setSchoolId(schoolId); return levyRepository.save(levy); }
    public FeeLevy updateLevy(String schoolId, String id, FeeLevy patch) {
        FeeLevy levy = levyRepository.findById(id).filter(item -> item.getSchoolId().equals(schoolId)).orElseThrow();
        if (patch.getName() != null) levy.setName(patch.getName());
        if (patch.getAmount() != null) levy.setAmount(patch.getAmount());
        if (patch.getGrade() != null) levy.setGrade(patch.getGrade());
        if (patch.getMandatory() != null) levy.setMandatory(patch.getMandatory());
        if (patch.getDescription() != null) levy.setDescription(patch.getDescription());
        if (patch.getApplicableTo() != null) levy.setApplicableTo(patch.getApplicableTo());
        if (patch.getEffectiveFrom() != null) levy.setEffectiveFrom(patch.getEffectiveFrom());
        return levyRepository.save(levy);
    }
    public void deleteLevy(String schoolId, String id) {
        FeeLevy levy = levyRepository.findById(id).filter(item -> item.getSchoolId().equals(schoolId)).orElseThrow();
        levyRepository.delete(levy);
    }

    public List<FeeDiscountRule> getDiscountRules(String schoolId) { return discountRuleRepository.findBySchoolIdOrderByCreatedAtDesc(schoolId); }
    public FeeDiscountRule createDiscountRule(String schoolId, FeeDiscountRule rule) { rule.setSchoolId(schoolId); return discountRuleRepository.save(rule); }
    public FeeDiscountRule updateDiscountRule(String schoolId, String id, FeeDiscountRule patch) {
        FeeDiscountRule rule = discountRuleRepository.findById(id).filter(item -> item.getSchoolId().equals(schoolId)).orElseThrow();
        if (patch.getName() != null) rule.setName(patch.getName());
        if (patch.getType() != null) rule.setType(patch.getType());
        if (patch.getValue() != null) rule.setValue(patch.getValue());
        if (patch.getCondition() != null) rule.setCondition(patch.getCondition());
        if (patch.getActive() != null) rule.setActive(patch.getActive());
        if (patch.getMaxBeneficiaries() != null) rule.setMaxBeneficiaries(patch.getMaxBeneficiaries());
        if (patch.getValidFrom() != null) rule.setValidFrom(patch.getValidFrom());
        if (patch.getValidTo() != null) rule.setValidTo(patch.getValidTo());
        if (patch.getRequiresBoardApproval() != null) rule.setRequiresBoardApproval(patch.getRequiresBoardApproval());
        return discountRuleRepository.save(rule);
    }

    public List<FeeBillingRule> getBillingRules(String schoolId) { return billingRuleRepository.findBySchoolIdOrderByDueDateAsc(schoolId); }
    public FeeBillingRule createBillingRule(String schoolId, FeeBillingRule rule) { rule.setSchoolId(schoolId); return billingRuleRepository.save(rule); }
    public FeeBillingRule updateBillingRule(String schoolId, String id, FeeBillingRule patch) {
        FeeBillingRule rule = billingRuleRepository.findById(id).filter(item -> item.getSchoolId().equals(schoolId)).orElseThrow();
        if (patch.getTerm() != null) rule.setTerm(patch.getTerm());
        if (patch.getDueDate() != null) rule.setDueDate(patch.getDueDate());
        if (patch.getLateFee() != null) rule.setLateFee(patch.getLateFee());
        if (patch.getReminderDays() != null) rule.setReminderDays(patch.getReminderDays());
        if (patch.getGracePeriodDays() != null) rule.setGracePeriodDays(patch.getGracePeriodDays());
        if (patch.getLateFeeMethod() != null) rule.setLateFeeMethod(patch.getLateFeeMethod());
        if (patch.getMaxPenalty() != null) rule.setMaxPenalty(patch.getMaxPenalty());
        return billingRuleRepository.save(rule);
    }
}
