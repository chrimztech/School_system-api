package com.srms.api.modules.accounting.service;

import com.srms.api.modules.accounting.entity.Expense;
import com.srms.api.modules.accounting.entity.JournalEntry;
import com.srms.api.modules.accounting.repository.ExpenseRepository;
import com.srms.api.modules.accounting.repository.JournalEntryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.List;

@Service @RequiredArgsConstructor
public class AccountingService {
    private final JournalEntryRepository journalRepo;
    private final ExpenseRepository expenseRepo;

    public List<JournalEntry> getJournalEntries(String schoolId) {
        return journalRepo.findBySchoolIdOrderByEntryDateDesc(schoolId);
    }

    public JournalEntry createJournalEntry(String schoolId, JournalEntry entry) {
        entry.setSchoolId(schoolId);
        if (entry.getEntryDate() == null) entry.setEntryDate(LocalDate.now());
        if (entry.getStatus() == null) entry.setStatus(JournalEntry.JournalStatus.DRAFT);
        return journalRepo.save(entry);
    }

    public JournalEntry postJournalEntry(String schoolId, String id) {
        JournalEntry entry = journalRepo.findById(id)
            .filter(e -> e.getSchoolId().equals(schoolId))
            .orElseThrow(() -> new RuntimeException("Journal entry not found"));
        entry.setStatus(JournalEntry.JournalStatus.POSTED);
        return journalRepo.save(entry);
    }

    public List<Expense> getExpenses(String schoolId) {
        return expenseRepo.findBySchoolIdOrderByExpenseDateDesc(schoolId);
    }

    public Expense createExpense(String schoolId, Expense expense) {
        expense.setSchoolId(schoolId);
        if (expense.getExpenseDate() == null) expense.setExpenseDate(LocalDate.now());
        if (expense.getStatus() == null) expense.setStatus(Expense.ExpenseStatus.PENDING);
        return expenseRepo.save(expense);
    }
}
