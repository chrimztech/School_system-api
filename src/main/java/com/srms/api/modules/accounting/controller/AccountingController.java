package com.srms.api.modules.accounting.controller;

import com.srms.api.common.ApiResponse;
import com.srms.api.modules.accounting.entity.Expense;
import com.srms.api.modules.accounting.entity.JournalEntry;
import com.srms.api.modules.accounting.service.AccountingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/schools/{schoolId}/accounting")
@RequiredArgsConstructor
public class AccountingController {
    private final AccountingService accountingService;

    @GetMapping("/journal")
    public ResponseEntity<ApiResponse<List<JournalEntry>>> getJournal(@PathVariable String schoolId) {
        return ResponseEntity.ok(ApiResponse.ok(accountingService.getJournalEntries(schoolId)));
    }

    @PostMapping("/journal")
    public ResponseEntity<ApiResponse<JournalEntry>> createJournalEntry(@PathVariable String schoolId, @RequestBody JournalEntry entry) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(accountingService.createJournalEntry(schoolId, entry)));
    }

    @PatchMapping("/journal/{id}/post")
    public ResponseEntity<ApiResponse<JournalEntry>> postJournalEntry(@PathVariable String schoolId, @PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.ok(accountingService.postJournalEntry(schoolId, id)));
    }

    @GetMapping("/expenses")
    public ResponseEntity<ApiResponse<List<Expense>>> getExpenses(@PathVariable String schoolId) {
        return ResponseEntity.ok(ApiResponse.ok(accountingService.getExpenses(schoolId)));
    }

    @PostMapping("/expenses")
    public ResponseEntity<ApiResponse<Expense>> createExpense(@PathVariable String schoolId, @RequestBody Expense expense) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(accountingService.createExpense(schoolId, expense)));
    }
}
