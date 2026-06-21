package com.srms.api.modules.library.repository;

import com.srms.api.modules.library.entity.LibraryBook;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface LibraryBookRepository extends JpaRepository<LibraryBook, String> {
    List<LibraryBook> findBySchoolId(String schoolId);
    List<LibraryBook> findBySchoolIdAndStatus(String schoolId, LibraryBook.Status status);
}
