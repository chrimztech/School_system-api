package com.srms.api.modules.accounting.service;

import com.srms.api.modules.accounting.entity.BudgetLine;
import com.srms.api.modules.accounting.entity.ChartAccount;
import com.srms.api.modules.accounting.entity.Expense;
import com.srms.api.modules.accounting.entity.FixedAsset;
import com.srms.api.modules.accounting.entity.JournalEntry;
import com.srms.api.modules.accounting.repository.BudgetLineRepository;
import com.srms.api.modules.accounting.repository.ChartAccountRepository;
import com.srms.api.modules.accounting.repository.ExpenseRepository;
import com.srms.api.modules.accounting.repository.FixedAssetRepository;
import com.srms.api.modules.accounting.repository.JournalEntryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Year;
import java.util.List;

@Service @RequiredArgsConstructor
public class AccountingService {
    private final JournalEntryRepository journalRepo;
    private final ExpenseRepository expenseRepo;
    private final ChartAccountRepository chartAccountRepo;
    private final BudgetLineRepository budgetLineRepo;
    private final FixedAssetRepository fixedAssetRepo;

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

    // ── Chart of accounts ────────────────────────────────────────
    public List<ChartAccount> getChartOfAccounts(String schoolId) {
        return chartAccountRepo.findBySchoolIdOrderByCodeAsc(schoolId);
    }

    public ChartAccount createChartAccount(String schoolId, ChartAccount account) {
        account.setSchoolId(schoolId);
        if (account.getBalance() == null) account.setBalance(BigDecimal.ZERO);
        return chartAccountRepo.save(account);
    }

    // ── Budgets ───────────────────────────────────────────────────
    public List<BudgetLine> getBudgetLines(String schoolId) {
        return budgetLineRepo.findBySchoolIdOrderByCategoryAsc(schoolId);
    }

    public BudgetLine createBudgetLine(String schoolId, BudgetLine line) {
        line.setSchoolId(schoolId);
        if (line.getSpent() == null) line.setSpent(BigDecimal.ZERO);
        if (line.getAcademicYear() == null) line.setAcademicYear(Year.now().getValue());
        return budgetLineRepo.save(line);
    }

    // ── Fixed assets ──────────────────────────────────────────────
    public List<FixedAsset> getFixedAssets(String schoolId) {
        return fixedAssetRepo.findBySchoolIdOrderByPurchaseDateDesc(schoolId);
    }

    public FixedAsset createFixedAsset(String schoolId, FixedAsset asset) {
        asset.setSchoolId(schoolId);
        if (asset.getPurchaseDate() == null) asset.setPurchaseDate(LocalDate.now());
        if (asset.getCondition() == null) asset.setCondition("Good");
        return fixedAssetRepo.save(asset);
    }
}
