package com.srms.api.modules.accounting.repository;

import com.srms.api.modules.accounting.entity.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, String> {
    List<Expense> findBySchoolIdOrderByExpenseDateDesc(String schoolId);
}
