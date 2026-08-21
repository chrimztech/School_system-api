package com.srms.api.modules.accounting.repository;

import com.srms.api.modules.accounting.entity.BudgetLine;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BudgetLineRepository extends JpaRepository<BudgetLine, String> {
    List<BudgetLine> findBySchoolIdOrderByCategoryAsc(String schoolId);
}
