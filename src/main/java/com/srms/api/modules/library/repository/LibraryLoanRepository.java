package com.srms.api.modules.library.repository;

import com.srms.api.modules.library.entity.LibraryLoan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface LibraryLoanRepository extends JpaRepository<LibraryLoan, String> {
    List<LibraryLoan> findBySchoolId(String schoolId);
    List<LibraryLoan> findBySchoolIdAndStatus(String schoolId, LibraryLoan.Status status);
    List<LibraryLoan> findBySchoolIdAndBorrowerId(String schoolId, String borrowerId);
}
