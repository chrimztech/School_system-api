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
    public List<FeePayment> getStudentPayments(String schoolId, String studentId) { return paymentRepository.findBySchoolIdAndStudentId(schoolId, studentId); }
    public FeePayment recordPayment(String schoolId, FeePayment payment) {
        payment.setSchoolId(schoolId);
        payment.setStatus(FeePayment.PaymentStatus.completed);
        FeePayment saved = paymentRepository.save(payment);
        studentRepository.findByIdAndSchoolId(payment.getStudentId(), schoolId).ifPresent(student -> {
            double newBalance = Math.max(0.0, student.getFeeBalance() - payment.getAmount());
            student.setFeeBalance(newBalance);
            studentRepository.save(student);
        });
        return saved;
    }
    public double getTotalCollected(String schoolId) { Double sum = paymentRepository.sumCollected(schoolId); return sum != null ? sum : 0; }
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
        return structureRepository.save(structure);
    }

    public List<FeeLevy> getLevies(String schoolId) { return levyRepository.findBySchoolIdOrderByCreatedAtDesc(schoolId); }
    public FeeLevy createLevy(String schoolId, FeeLevy levy) { levy.setSchoolId(schoolId); return levyRepository.save(levy); }
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
