package com.srms.api.modules.fee.repository;

import com.srms.api.modules.fee.entity.FeeBillingRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FeeBillingRuleRepository extends JpaRepository<FeeBillingRule, String> {
    List<FeeBillingRule> findBySchoolIdOrderByDueDateAsc(String schoolId);
}
