package com.srms.api.modules.fee.repository;

import com.srms.api.modules.fee.entity.FeeDiscountRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FeeDiscountRuleRepository extends JpaRepository<FeeDiscountRule, String> {
    List<FeeDiscountRule> findBySchoolIdOrderByCreatedAtDesc(String schoolId);
}
