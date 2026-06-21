package com.srms.api.modules.library.controller;

import com.srms.api.common.ApiResponse;
import com.srms.api.modules.library.entity.LibraryBook;
import com.srms.api.modules.library.entity.LibraryLoan;
import com.srms.api.modules.library.service.LibraryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/schools/{schoolId}/library")
@RequiredArgsConstructor
public class LibraryController {

    private final LibraryService libraryService;

    // ── Books ──────────────────────────────────────────────

    @GetMapping("/books")
    public ResponseEntity<ApiResponse<List<LibraryBook>>> getAllBooks(@PathVariable String schoolId) {
        return ResponseEntity.ok(ApiResponse.ok(libraryService.getAllBooks(schoolId)));
    }

    @PostMapping("/books")
    public ResponseEntity<ApiResponse<LibraryBook>> createBook(
            @PathVariable String schoolId,
            @RequestBody LibraryBook book) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(libraryService.createBook(schoolId, book)));
    }

    @PutMapping("/books/{id}")
    public ResponseEntity<ApiResponse<LibraryBook>> updateBook(
            @PathVariable String schoolId,
            @PathVariable String id,
            @RequestBody LibraryBook book) {
        return ResponseEntity.ok(ApiResponse.ok("Book updated", libraryService.updateBook(schoolId, id, book)));
    }

    @DeleteMapping("/books/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteBook(
            @PathVariable String schoolId,
            @PathVariable String id) {
        libraryService.deleteBook(schoolId, id);
        return ResponseEntity.ok(ApiResponse.ok("Book deleted", null));
    }

    // ── Loans ─────────────────────────────────────────────

    @GetMapping("/loans")
    public ResponseEntity<ApiResponse<List<LibraryLoan>>> getAllLoans(@PathVariable String schoolId) {
        return ResponseEntity.ok(ApiResponse.ok(libraryService.getAllLoans(schoolId)));
    }

    @PostMapping("/loans")
    public ResponseEntity<ApiResponse<LibraryLoan>> issueLoan(
            @PathVariable String schoolId,
            @RequestBody LibraryLoan loan) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(libraryService.issueLoan(schoolId, loan)));
    }

    @PutMapping("/loans/{id}/return")
    public ResponseEntity<ApiResponse<LibraryLoan>> returnLoan(
            @PathVariable String schoolId,
            @PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.ok("Loan returned", libraryService.returnLoan(schoolId, id)));
    }
}
