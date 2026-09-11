package com.srms.api.modules.accounting.controller;

import com.srms.api.common.ApiResponse;
import com.srms.api.exception.ForbiddenException;
import com.srms.api.modules.accounting.entity.BudgetLine;
import com.srms.api.modules.accounting.entity.ChartAccount;
import com.srms.api.modules.accounting.entity.Expense;
import com.srms.api.modules.accounting.entity.FixedAsset;
import com.srms.api.modules.accounting.entity.JournalEntry;
import com.srms.api.modules.accounting.service.AccountingService;
import com.srms.api.security.ModuleAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Set;

/** The school's internal general ledger (journal, expenses, chart of accounts, budgets, fixed
 * assets) — "accounting" is "true" only for FINANCE and leadership in the frontend's own
 * access matrix, false for everyone else including TEACHER/HOD/PARENT/CAREER_GUIDANCE. None of
 * this is per-student, so it's a flat gate rather than an ownership check. */
@RestController
@RequestMapping("/api/schools/{schoolId}/accounting")
@RequiredArgsConstructor
public class AccountingController {
    private final AccountingService accountingService;
    private final ModuleAccessService moduleAccessService;

    private static final Set<String> FULL_ROLES = Set.of(
            "SUPER_ADMIN", "SCHOOL_ADMIN", "PRINCIPAL", "DEPUTY_HEAD", "FINANCE");

    private void requireFull(String schoolId, Authentication auth) {
        String role = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst().map(a -> a.replaceFirst("^ROLE_", "")).orElse("");
        if (!moduleAccessService.isAllowed(schoolId, auth, "accounting", "full", FULL_ROLES.contains(role))) {
            throw new ForbiddenException("Your role cannot access accounting records");
        }
    }

    @GetMapping("/journal")
    public ResponseEntity<ApiResponse<List<JournalEntry>>> getJournal(@PathVariable String schoolId, Authentication auth) {
        requireFull(schoolId, auth);
        return ResponseEntity.ok(ApiResponse.ok(accountingService.getJournalEntries(schoolId)));
    }

    @PostMapping("/journal")
    public ResponseEntity<ApiResponse<JournalEntry>> createJournalEntry(@PathVariable String schoolId, @RequestBody JournalEntry entry, Authentication auth) {
        requireFull(schoolId, auth);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(accountingService.createJournalEntry(schoolId, entry)));
    }

    @PatchMapping("/journal/{id}/post")
    public ResponseEntity<ApiResponse<JournalEntry>> postJournalEntry(@PathVariable String schoolId, @PathVariable String id, Authentication auth) {
        requireFull(schoolId, auth);
        return ResponseEntity.ok(ApiResponse.ok(accountingService.postJournalEntry(schoolId, id)));
    }

    @GetMapping("/expenses")
    public ResponseEntity<ApiResponse<List<Expense>>> getExpenses(@PathVariable String schoolId, Authentication auth) {
        requireFull(schoolId, auth);
        return ResponseEntity.ok(ApiResponse.ok(accountingService.getExpenses(schoolId)));
    }

    @PostMapping("/expenses")
    public ResponseEntity<ApiResponse<Expense>> createExpense(@PathVariable String schoolId, @RequestBody Expense expense, Authentication auth) {
        requireFull(schoolId, auth);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(accountingService.createExpense(schoolId, expense)));
    }

    @GetMapping("/chart-of-accounts")
    public ResponseEntity<ApiResponse<List<ChartAccount>>> getChartOfAccounts(@PathVariable String schoolId, Authentication auth) {
        requireFull(schoolId, auth);
        return ResponseEntity.ok(ApiResponse.ok(accountingService.getChartOfAccounts(schoolId)));
    }

    @PostMapping("/chart-of-accounts")
    public ResponseEntity<ApiResponse<ChartAccount>> createChartAccount(@PathVariable String schoolId, @RequestBody ChartAccount account, Authentication auth) {
        requireFull(schoolId, auth);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(accountingService.createChartAccount(schoolId, account)));
    }

    @GetMapping("/budgets")
    public ResponseEntity<ApiResponse<List<BudgetLine>>> getBudgetLines(@PathVariable String schoolId, Authentication auth) {
        requireFull(schoolId, auth);
        return ResponseEntity.ok(ApiResponse.ok(accountingService.getBudgetLines(schoolId)));
    }

    @PostMapping("/budgets")
    public ResponseEntity<ApiResponse<BudgetLine>> createBudgetLine(@PathVariable String schoolId, @RequestBody BudgetLine line, Authentication auth) {
        requireFull(schoolId, auth);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(accountingService.createBudgetLine(schoolId, line)));
    }

    @GetMapping("/fixed-assets")
    public ResponseEntity<ApiResponse<List<FixedAsset>>> getFixedAssets(@PathVariable String schoolId, Authentication auth) {
        requireFull(schoolId, auth);
        return ResponseEntity.ok(ApiResponse.ok(accountingService.getFixedAssets(schoolId)));
    }

    @PostMapping("/fixed-assets")
    public ResponseEntity<ApiResponse<FixedAsset>> createFixedAsset(@PathVariable String schoolId, @RequestBody FixedAsset asset, Authentication auth) {
        requireFull(schoolId, auth);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(accountingService.createFixedAsset(schoolId, asset)));
    }
}
