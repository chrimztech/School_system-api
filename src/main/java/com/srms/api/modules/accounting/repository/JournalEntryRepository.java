package com.srms.api.modules.accounting.repository;

import com.srms.api.modules.accounting.entity.JournalEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface JournalEntryRepository extends JpaRepository<JournalEntry, String> {
    List<JournalEntry> findBySchoolIdOrderByEntryDateDesc(String schoolId);
}
