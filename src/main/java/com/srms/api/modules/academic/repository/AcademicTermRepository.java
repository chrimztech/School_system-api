package com.srms.api.modules.academic.repository;

import com.srms.api.modules.academic.entity.AcademicTerm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AcademicTermRepository extends JpaRepository<AcademicTerm, String> {
    List<AcademicTerm> findBySchoolIdOrderByAcademicYearDescTermAsc(String schoolId);
    List<AcademicTerm> findBySchoolIdAndAcademicYearOrderByTermAsc(String schoolId, int academicYear);
    Optional<AcademicTerm> findBySchoolIdAndAcademicYearAndTerm(String schoolId, int academicYear, int term);

    // Which defined term(s) cover a given calendar date — the basis for auto-advancing
    // School.currentTerm/currentYear off the calendar instead of a manually-set number.
    // A List, not an Optional<AcademicTerm>: nothing stops two terms' date ranges from being
    // entered overlapping by mistake, and a single-result derived query throws if more than
    // one row matches — the service picks the first rather than that failing outright.
    List<AcademicTerm> findBySchoolIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            String schoolId, LocalDate onOrAfterStart, LocalDate onOrBeforeEnd);
}
