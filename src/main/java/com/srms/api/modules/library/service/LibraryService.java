package com.srms.api.modules.library.service;

import com.srms.api.modules.library.entity.LibraryBook;
import com.srms.api.modules.library.entity.LibraryLoan;
import com.srms.api.modules.library.repository.LibraryBookRepository;
import com.srms.api.modules.library.repository.LibraryLoanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class LibraryService {

    private final LibraryBookRepository bookRepository;
    private final LibraryLoanRepository loanRepository;

    // ── Books ──────────────────────────────────────────────

    public List<LibraryBook> getAllBooks(String schoolId) {
        return bookRepository.findBySchoolId(schoolId);
    }

    public LibraryBook createBook(String schoolId, LibraryBook book) {
        book.setSchoolId(schoolId);
        if (book.getAvailableCopies() == 0) {
            book.setAvailableCopies(book.getTotalCopies());
        }
        book.setStatus(book.getAvailableCopies() > 0 ? LibraryBook.Status.AVAILABLE : LibraryBook.Status.ALL_BORROWED);
        return bookRepository.save(book);
    }

    public LibraryBook updateBook(String schoolId, String id, LibraryBook updated) {
        LibraryBook book = bookRepository.findById(id)
                .filter(b -> b.getSchoolId().equals(schoolId))
                .orElseThrow(() -> new RuntimeException("Book not found"));
        book.setTitle(updated.getTitle());
        book.setAuthor(updated.getAuthor());
        book.setCategory(updated.getCategory());
        book.setPublisher(updated.getPublisher());
        book.setYearPublished(updated.getYearPublished());
        book.setTotalCopies(updated.getTotalCopies());
        book.setAvailableCopies(updated.getAvailableCopies());
        book.setLocation(updated.getLocation());
        book.setStatus(updated.getAvailableCopies() > 0 ? LibraryBook.Status.AVAILABLE : LibraryBook.Status.ALL_BORROWED);
        return bookRepository.save(book);
    }

    public void deleteBook(String schoolId, String id) {
        LibraryBook book = bookRepository.findById(id)
                .filter(b -> b.getSchoolId().equals(schoolId))
                .orElseThrow(() -> new RuntimeException("Book not found"));
        bookRepository.delete(book);
    }

    // ── Loans ─────────────────────────────────────────────

    public List<LibraryLoan> getAllLoans(String schoolId) {
        return loanRepository.findBySchoolId(schoolId);
    }

    public LibraryLoan issueLoan(String schoolId, LibraryLoan loan) {
        loan.setSchoolId(schoolId);
        loan.setStatus(LibraryLoan.Status.ACTIVE);
        if (loan.getLoanDate() == null) {
            loan.setLoanDate(LocalDate.now());
        }
        // Decrement available copies
        if (loan.getBookId() != null) {
            bookRepository.findById(loan.getBookId()).ifPresent(book -> {
                int available = book.getAvailableCopies();
                if (available <= 0) throw new RuntimeException("No copies available for borrowing");
                book.setAvailableCopies(available - 1);
                book.setStatus(book.getAvailableCopies() == 0 ? LibraryBook.Status.ALL_BORROWED : LibraryBook.Status.AVAILABLE);
                bookRepository.save(book);
            });
        }
        return loanRepository.save(loan);
    }

    public LibraryLoan returnLoan(String schoolId, String loanId) {
        LibraryLoan loan = loanRepository.findById(loanId)
                .filter(l -> l.getSchoolId().equals(schoolId))
                .orElseThrow(() -> new RuntimeException("Loan not found"));
        loan.setReturnDate(LocalDate.now());
        loan.setStatus(LibraryLoan.Status.RETURNED);
        // Increment available copies
        if (loan.getBookId() != null) {
            bookRepository.findById(loan.getBookId()).ifPresent(book -> {
                book.setAvailableCopies(book.getAvailableCopies() + 1);
                book.setStatus(LibraryBook.Status.AVAILABLE);
                bookRepository.save(book);
            });
        }
        return loanRepository.save(loan);
    }
}
